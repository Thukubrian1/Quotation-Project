package com.paymentservice.paymentservice.ServiceImpl;

import com.paymentservice.paymentservice.Config.MpesaProperties;
import com.paymentservice.paymentservice.DTOs.*;
import com.paymentservice.paymentservice.Entity.PaymentTransaction;
import com.paymentservice.paymentservice.Repository.PaymentTransactionRepository;
import com.paymentservice.paymentservice.Service.MpesaService;
import com.paymentservice.paymentservice.Service.PaymentWebSocketService;
import com.shared.sharedlib.Enums.PaymentStatus;
import com.shared.sharedlib.Enums.ResponseStatusEnum;
import com.shared.sharedlib.Exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MpesaServiceImpl implements MpesaService {

    @Qualifier("mpesaRestTemplate")
    private final RestTemplate mpesaRestTemplate;

    private final MpesaProperties mpesaProperties;
    private final PaymentTransactionRepository paymentRepository;
    private final PaymentWebSocketService paymentWebSocketService;

    @Override
    public PaymentResponse initiateSTKPush(PaymentRequest request) {
        try {
            log.info("Starting STK push process for phone: {}, amount: {}",
                    request.getPhoneNumber(), request.getAmount());

            validatePaymentRequest(request);
            String formattedPhoneNumber = formatPhoneNumber(request.getPhoneNumber());

            String accessToken = getMpesaAccessToken();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String password = generatePassword(timestamp);

            StkPushRequest stkRequest = createStkPushRequest(request, password, timestamp, formattedPhoneNumber);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<StkPushRequest> entity = new HttpEntity<>(stkRequest, headers);

            ResponseEntity<StkPushResponse> response = mpesaRestTemplate.exchange(
                    mpesaProperties.getStkPushRequestUrl(), HttpMethod.POST, entity, StkPushResponse.class);

            StkPushResponse stkResponse = response.getBody();

            if (stkResponse == null || !"0".equals(stkResponse.getResponseCode())) {
                throw new BusinessException(
                        ResponseStatusEnum.BAD_REQUEST,
                        "STK push failed: " + (stkResponse != null ? stkResponse.getCustomerMessage() : "No response"),
                        "M-Pesa response code: " + (stkResponse != null ? stkResponse.getResponseCode() : "null"));
            }

            PaymentTransaction transaction = createPaymentTransaction(request, stkResponse);
            PaymentTransaction savedTransaction = paymentRepository.save(transaction);
            log.info("Payment transaction saved with ID: {}", savedTransaction.getId());

            sendPaymentInitiatedNotification(savedTransaction);

            // startPaymentStatusPolling(savedTransaction.getCheckoutRequestId()); //
            // Polling removed to use Webhooks only

            return PaymentResponse.builder()
                    .merchantRequestId(stkResponse.getMerchantRequestID())
                    .checkoutRequestId(stkResponse.getCheckoutRequestID())
                    .responseCode(stkResponse.getResponseCode())
                    .message(stkResponse.getCustomerMessage())
                    .status(PaymentStatus.PENDING.name())
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error initiating STK push", e);
            throw new BusinessException(
                    ResponseStatusEnum.ERROR,
                    "Failed to initiate payment",
                    e.getMessage());
        }
    }

    public StkQueryResponse queryPaymentStatus(String checkoutRequestId) {
        try {
            String accessToken = getMpesaAccessToken();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String password = generatePassword(timestamp);

            StkQueryRequest queryRequest = StkQueryRequest.builder()
                    .businessShortCode(mpesaProperties.getBusinessShortCode())
                    .password(password)
                    .timestamp(timestamp)
                    .checkoutRequestID(checkoutRequestId)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<StkQueryRequest> entity = new HttpEntity<>(queryRequest, headers);

            log.info("Querying M-Pesa for CheckoutRequestID: {}", checkoutRequestId);

            ResponseEntity<StkQueryResponse> response = mpesaRestTemplate.exchange(
                    mpesaProperties.getQueryStatusUrl(),
                    HttpMethod.POST,
                    entity,
                    StkQueryResponse.class);

            StkQueryResponse queryResponse = response.getBody();

            if (queryResponse != null) {
                log.info("Query response for {}: ResultCode={}, ResultDesc={}",
                        checkoutRequestId, queryResponse.getResultCode(), queryResponse.getResultDesc());
            }

            return queryResponse;

        } catch (Exception e) {
            log.error("Error querying payment status for {}", checkoutRequestId, e);
            throw new BusinessException(
                    ResponseStatusEnum.ERROR,
                    "Failed to query payment status",
                    e.getMessage());
        }
    }

    @Async
    public void startPaymentStatusPolling(String checkoutRequestId) {
        log.info("⏰ Starting payment status polling for: {}", checkoutRequestId);

        int maxAttempts = 40; // 40 attempts * 2 seconds = 80 seconds (~1.3 minutes)
        int attempt = 0;
        boolean completed = false;

        while (attempt < maxAttempts && !completed) {
            try {
                attempt++;
                Thread.sleep(2000); // Poll every 2 seconds

                log.info("📊 Polling attempt {}/{} for {}", attempt, maxAttempts, checkoutRequestId);

                StkQueryResponse queryResponse = queryPaymentStatus(checkoutRequestId);

                if (queryResponse != null) {
                    completed = processQueryResponse(checkoutRequestId, queryResponse);

                    if (completed) {
                        log.info("✅ Polling completed for {} after {} attempts", checkoutRequestId, attempt);
                        return; // Exit immediately when done
                    }
                }

            } catch (InterruptedException e) {
                log.warn("⚠️ Polling interrupted for {}", checkoutRequestId);
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("❌ Error during polling attempt {} for {}", attempt, checkoutRequestId, e);
                // Continue polling despite errors
            }
        }

        // Only handle timeout if we exhausted all attempts without completion
        if (!completed && attempt >= maxAttempts) {
            log.warn("⏱️ Payment polling timed out for {} after {} attempts", checkoutRequestId, maxAttempts);
            handlePaymentTimeout(checkoutRequestId);
        }
    }

    private boolean processQueryResponse(String checkoutRequestId, StkQueryResponse queryResponse) {
        try {
            Optional<PaymentTransaction> transactionOpt = paymentRepository.findByCheckoutRequestId(checkoutRequestId);

            if (transactionOpt.isEmpty()) {
                log.warn("Transaction not found for {}", checkoutRequestId);
                return true; // Stop polling
            }

            PaymentTransaction transaction = transactionOpt.get();

            // Don't process if already in final state
            if (isFinalState(transaction.getStatus())) {
                log.info("Transaction already in final state: {}", transaction.getStatus());
                return true;
            }

            String resultCode = queryResponse.getResultCode();

            if (resultCode == null || resultCode.isEmpty()) {
                log.debug("Empty result code, continuing polling...");
                return false;
            }

            int code = Integer.parseInt(resultCode);
            log.info("Processing result code: {} for {}", code, checkoutRequestId);

            // SUCCESS - Code 0
            if (code == 0) {
                transaction.setStatus(PaymentStatus.COMPLETED);
                transaction.setMpesaReceiptNumber(queryResponse.getMpesaReceiptNumber());
                transaction.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(transaction);

                sendPaymentUpdate(transaction, PaymentStatus.COMPLETED,
                        "Payment completed successfully", code);

                log.info("✅ Payment COMPLETED for {}", checkoutRequestId);
                return true; // STOP POLLING
            }

            // CANCELLED - Codes 1032, 1036, 17
            else if (code == 1032 || code == 1036 || code == 17) {
                transaction.setStatus(PaymentStatus.CANCELLED);
                transaction.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(transaction);

                sendPaymentUpdate(transaction, PaymentStatus.CANCELLED,
                        "Payment cancelled by user", code);

                log.info("🚫 Payment CANCELLED for {}", checkoutRequestId);
                return true; // STOP POLLING
            }

            // PENDING - Code 1037 (DS timeout, keep waiting)
            else if (code == 1037) {
                log.debug("Code 1037 - Transaction still pending, continue polling...");

                // Optionally send a status update
                sendPaymentUpdate(transaction, PaymentStatus.PENDING,
                        "Waiting for user to complete payment on their phone", code);

                return false; // KEEP POLLING
            }

            // FAILED - All other non-zero codes
            else {
                PaymentStatusInfo statusInfo = mapResultCodeToStatusInfo(code, queryResponse.getResultDesc());
                transaction.setStatus(statusInfo.getStatus());
                transaction.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(transaction);

                sendPaymentUpdate(transaction, statusInfo.getStatus(),
                        statusInfo.getMessage(), code);

                log.info("❌ Payment {} for {}: {}", statusInfo.getStatus(), checkoutRequestId, statusInfo.getMessage());
                return true; // STOP POLLING
            }

        } catch (NumberFormatException e) {
            log.error("Invalid result code format for {}: {}", checkoutRequestId, queryResponse.getResultCode());
            return false; // Continue polling
        } catch (Exception e) {
            log.error("Error processing query response for {}", checkoutRequestId, e);
            return false; // Continue polling despite error
        }
    }

    private boolean isFinalState(PaymentStatus status) {
        return status == PaymentStatus.COMPLETED ||
                status == PaymentStatus.FAILED ||
                status == PaymentStatus.CANCELLED ||
                status == PaymentStatus.EXPIRED;
    }

    private void handlePaymentTimeout(String checkoutRequestId) {
        try {
            Optional<PaymentTransaction> transactionOpt = paymentRepository.findByCheckoutRequestId(checkoutRequestId);

            if (transactionOpt.isEmpty()) {
                return;
            }

            PaymentTransaction transaction = transactionOpt.get();

            // Only update if still pending
            if (transaction.getStatus() == PaymentStatus.PENDING) {
                transaction.setStatus(PaymentStatus.EXPIRED);
                transaction.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(transaction);

                sendPaymentUpdate(transaction, PaymentStatus.EXPIRED,
                        "Payment request expired - no response received from user", 1037);

                log.info("⏱️ Payment EXPIRED for {}", checkoutRequestId);
            }

        } catch (Exception e) {
            log.error("Error handling timeout for {}", checkoutRequestId, e);
        }
    }

    @Override
    public void handleCallback(StkCallback callback) {
        try {
            if (callback == null || callback.getBody() == null || callback.getBody().getStkCallback() == null) {
                log.warn("Received null or invalid callback data");
                return;
            }

            StkCallback.StkCallbackData callbackData = callback.getBody().getStkCallback();
            String checkoutRequestId = callbackData.getCheckoutRequestID();
            Integer resultCode = callbackData.getResultCode();

            log.info("Processing callback for CheckoutRequestID: {}, ResultCode: {}",
                    checkoutRequestId, resultCode);

            Optional<PaymentTransaction> transactionOpt = paymentRepository.findByCheckoutRequestId(checkoutRequestId);

            if (transactionOpt.isEmpty()) {
                log.warn("No transaction found for CheckoutRequestID: {}", checkoutRequestId);
                return;
            }

            PaymentTransaction transaction = transactionOpt.get();

            // Only process if not already in final state
            if (!isFinalState(transaction.getStatus())) {
                PaymentStatusInfo statusInfo = mapResultCodeToStatusInfo(resultCode, callbackData.getResultDesc());
                transaction.setStatus(statusInfo.getStatus());

                if (resultCode == 0) {
                    extractCallbackMetadata(callbackData, transaction);
                }

                transaction.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(transaction);

                sendPaymentUpdate(transaction, statusInfo.getStatus(),
                        statusInfo.getMessage(), resultCode);

                log.info("Callback processed successfully for {}", checkoutRequestId);
            } else {
                log.info("Transaction already processed via polling for {}", checkoutRequestId);
            }

        } catch (Exception e) {
            log.error("Error processing callback", e);
        }
    }

    @Override
    public PaymentStatusResponse getPaymentStatus(String checkoutRequestId) {
        log.info("Retrieving payment status for CheckoutRequestID: {}", checkoutRequestId);

        Optional<PaymentTransaction> transactionOpt = paymentRepository.findByCheckoutRequestId(checkoutRequestId);

        if (transactionOpt.isEmpty()) {
            throw new BusinessException(
                    ResponseStatusEnum.NOT_FOUND,
                    "Transaction not found",
                    "No transaction found for CheckoutRequestID: " + checkoutRequestId);
        }

        PaymentTransaction transaction = transactionOpt.get();

        // If validation is needed and status is PENDING, actively query M-Pesa
        if (!isFinalState(transaction.getStatus())) {
            try {
                log.info("Transaction {} is PENDING, actively querying M-Pesa...", checkoutRequestId);
                StkQueryResponse queryResponse = queryPaymentStatus(checkoutRequestId);

                if (queryResponse != null) {
                    // Update DB and send notifications
                    boolean completed = processQueryResponse(checkoutRequestId, queryResponse);
                    if (completed) {
                        // Re-fetch updated transaction to return correct status
                        transaction = paymentRepository.findByCheckoutRequestId(checkoutRequestId).orElse(transaction);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to query M-Pesa during status check for {}", checkoutRequestId, e);
                // Continue with existing status if query fails
            }
        }

        return PaymentStatusResponse.builder()
                .checkoutRequestId(transaction.getCheckoutRequestId())
                .merchantRequestId(transaction.getMerchantRequestId())
                .phoneNumber(transaction.getPhoneNumber())
                .amount(transaction.getAmount())
                .accountReference(transaction.getAccountReference())
                .status(transaction.getStatus().name())
                .statusMessage(getStatusMessage(transaction.getStatus()))
                .mpesaReceiptNumber(transaction.getMpesaReceiptNumber())
                .transactionDate(transaction.getTransactionDate())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }

    private void sendPaymentInitiatedNotification(PaymentTransaction transaction) {
        try {
            PaymentStatusMessage statusMessage = PaymentStatusMessage.builder()
                    .checkoutRequestId(transaction.getCheckoutRequestId())
                    .merchantRequestId(transaction.getMerchantRequestId())
                    .phoneNumber(transaction.getPhoneNumber())
                    .amount(transaction.getAmount())
                    .accountReference(transaction.getAccountReference())
                    .status(PaymentStatus.PENDING.name())
                    .statusMessage("Payment request sent. Check your phone to complete the transaction.")
                    .transactionDate(transaction.getTransactionDate())
                    .updatedAt(transaction.getUpdatedAt())
                    .eventType("INITIATED")
                    .build();

            paymentWebSocketService.sendPaymentUpdate(transaction.getCheckoutRequestId(), statusMessage);
            log.info("📤 Payment initiated notification sent for {}", transaction.getCheckoutRequestId());

        } catch (Exception e) {
            log.error("Failed to send payment initiated notification", e);
        }
    }

    private void sendPaymentUpdate(PaymentTransaction transaction, PaymentStatus status,
            String message, Integer resultCode) {
        try {
            String eventType = determineEventType(status);

            PaymentStatusMessage statusMessage = PaymentStatusMessage.builder()
                    .checkoutRequestId(transaction.getCheckoutRequestId())
                    .merchantRequestId(transaction.getMerchantRequestId())
                    .phoneNumber(transaction.getPhoneNumber())
                    .amount(transaction.getAmount())
                    .accountReference(transaction.getAccountReference())
                    .status(status.name())
                    .statusMessage(message)
                    .mpesaReceiptNumber(transaction.getMpesaReceiptNumber())
                    .transactionDate(transaction.getTransactionDate())
                    .updatedAt(transaction.getUpdatedAt())
                    .resultCode(resultCode)
                    .eventType(eventType)
                    .build();

            paymentWebSocketService.sendPaymentUpdate(transaction.getCheckoutRequestId(), statusMessage);
            log.info("📤 Payment update sent for {}: status={}, resultCode={}",
                    transaction.getCheckoutRequestId(), status, resultCode);

        } catch (Exception e) {
            log.error("Failed to send payment update", e);
        }
    }

    private String determineEventType(PaymentStatus status) {
        switch (status) {
            case COMPLETED:
                return "COMPLETED";
            case FAILED:
                return "FAILED";
            case CANCELLED:
                return "CANCELLED";
            case EXPIRED:
                return "EXPIRED";
            case PENDING:
                return "UPDATED";
            default:
                return "UPDATED";
        }
    }

    private PaymentStatusInfo mapResultCodeToStatusInfo(Integer resultCode, String resultDesc) {
        if (resultCode == null) {
            return new PaymentStatusInfo(PaymentStatus.FAILED, "Payment failed - unknown error");
        }

        switch (resultCode) {
            case 0:
                return new PaymentStatusInfo(PaymentStatus.COMPLETED, "Payment completed successfully");
            case 1:
                return new PaymentStatusInfo(PaymentStatus.FAILED, "Insufficient funds in your M-Pesa account");
            case 17:
            case 1032:
            case 1036:
                return new PaymentStatusInfo(PaymentStatus.CANCELLED, "Payment cancelled by user");
            case 1037:
                return new PaymentStatusInfo(PaymentStatus.PENDING, "Waiting for user to complete payment");
            case 1012:
                return new PaymentStatusInfo(PaymentStatus.EXPIRED, "Payment request timed out");
            case 2001:
                return new PaymentStatusInfo(PaymentStatus.FAILED, "Wrong M-Pesa PIN entered");
            case 4001:
                return new PaymentStatusInfo(PaymentStatus.FAILED, "Invalid merchant configuration");
            case 4909:
                return new PaymentStatusInfo(PaymentStatus.CANCELLED, "Payment cancelled - user declined");
            default:
                return new PaymentStatusInfo(PaymentStatus.FAILED,
                        "Payment failed: "
                                + (resultDesc != null ? resultDesc : "Unknown error (code: " + resultCode + ")"));
        }
    }

    private String getStatusMessage(PaymentStatus status) {
        switch (status) {
            case PENDING:
                return "Payment request sent. Check your phone and enter your M-Pesa PIN.";
            case COMPLETED:
                return "Payment completed successfully!";
            case FAILED:
                return "Payment failed. Please try again.";
            case CANCELLED:
                return "Payment was cancelled.";
            case EXPIRED:
                return "Payment request expired. Please try again.";
            default:
                return "Payment status unknown.";
        }
    }

    private static class PaymentStatusInfo {
        private final PaymentStatus status;
        private final String message;

        public PaymentStatusInfo(PaymentStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        public PaymentStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }

    private String formatPhoneNumber(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            throw new BusinessException(ResponseStatusEnum.BAD_REQUEST, "Phone number is required", "");
        }
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("254") && cleaned.length() == 12)
            return cleaned;
        if (cleaned.startsWith("0") && cleaned.length() == 10)
            return "254" + cleaned.substring(1);
        if (cleaned.length() == 9)
            return "254" + cleaned;
        throw new BusinessException(ResponseStatusEnum.BAD_REQUEST, "Invalid phone number format", "");
    }

    private void validatePaymentRequest(PaymentRequest request) {
        if (request == null || !StringUtils.hasText(request.getPhoneNumber()) ||
                request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResponseStatusEnum.BAD_REQUEST, "Invalid payment request", "");
        }
    }

    private PaymentTransaction createPaymentTransaction(PaymentRequest request, StkPushResponse stkResponse) {
        return PaymentTransaction.builder()
                .merchantRequestId(stkResponse.getMerchantRequestID())
                .checkoutRequestId(stkResponse.getCheckoutRequestID())
                .phoneNumber(request.getPhoneNumber())
                .amount(request.getAmount())
                .accountReference(request.getAccountReference())
                .transactionDescription(request.getTransactionDescription())
                .status(PaymentStatus.PENDING)
                .transactionDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private String getMpesaAccessToken() {
        try {
            String auth = mpesaProperties.getConsumerKey() + ":" + mpesaProperties.getConsumerSecret();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<AccessTokenResponse> response = mpesaRestTemplate.exchange(
                    mpesaProperties.getOauthEndpoint() + "?grant_type=client_credentials",
                    HttpMethod.GET, entity, AccessTokenResponse.class);

            if (response.getBody() == null || response.getBody().getAccessToken() == null) {
                throw new BusinessException(ResponseStatusEnum.ERROR, "Failed to obtain access token", "");
            }

            return response.getBody().getAccessToken();
        } catch (Exception e) {
            throw new BusinessException(ResponseStatusEnum.ERROR, "Failed to authenticate with M-Pesa", e.getMessage());
        }
    }

    private String generatePassword(String timestamp) {
        String raw = mpesaProperties.getBusinessShortCode() + mpesaProperties.getPassKey() + timestamp;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private StkPushRequest createStkPushRequest(PaymentRequest request, String password,
            String timestamp, String formattedPhoneNumber) {
        return StkPushRequest.builder()
                .businessShortCode(mpesaProperties.getBusinessShortCode())
                .password(password)
                .timestamp(timestamp)
                .transactionType("CustomerPayBillOnline")
                .amount(request.getAmount())
                .partyA(formattedPhoneNumber)
                .partyB(mpesaProperties.getBusinessShortCode())
                .phoneNumber(formattedPhoneNumber)
                .callBackURL(mpesaProperties.getCallbackUrl())
                .accountReference(request.getAccountReference())
                .transactionDesc(request.getTransactionDescription())
                .build();
    }

    private void extractCallbackMetadata(StkCallback.StkCallbackData callbackData, PaymentTransaction transaction) {
        if (callbackData.getCallbackMetadata() != null && callbackData.getCallbackMetadata().getItem() != null) {
            for (StkCallback.CallbackItem item : callbackData.getCallbackMetadata().getItem()) {
                if ("MpesaReceiptNumber".equals(item.getName()) && item.getValue() != null) {
                    transaction.setMpesaReceiptNumber(item.getValue().toString());
                }
            }
        }
    }
}