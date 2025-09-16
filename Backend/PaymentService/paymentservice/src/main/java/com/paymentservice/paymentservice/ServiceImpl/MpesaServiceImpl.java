package com.paymentservice.paymentservice.ServiceImpl;

import com.paymentservice.paymentservice.Config.MpesaProperties;
import com.paymentservice.paymentservice.DTOs.*;
import com.paymentservice.paymentservice.Entity.PaymentTransaction;
import com.paymentservice.paymentservice.Repository.PaymentTransactionRepository;
import com.paymentservice.paymentservice.Service.AuthServiceClient;
import com.paymentservice.paymentservice.Service.MpesaService;
import com.shared.sharedlib.Enums.PaymentStatus;
import com.shared.sharedlib.Enums.ResponseStatusEnum;
import com.shared.sharedlib.Exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
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

    @Qualifier("restTemplate") // Use the configured RestTemplate with bearer token
    private final RestTemplate authenticatedRestTemplate;

    @Qualifier("mpesaRestTemplate") // Use separate RestTemplate for M-Pesa API calls
    private final RestTemplate mpesaRestTemplate;

    @Value("${external.service.auth-service.base-url}")
    private String authServiceBaseUrl;

    private final MpesaProperties mpesaProperties;
    private final PaymentTransactionRepository paymentRepository;
    private final AuthServiceClient authServiceClient;

    /**
     * Formats Kenyan phone number to international format for M-Pesa API
     * Converts 0741819799 to 254741819799
     */
    private String formatPhoneNumber(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            throw new BusinessException(
                    ResponseStatusEnum.BAD_REQUEST,
                    "Phone number is required",
                    "phoneNumber cannot be null or empty"
            );
        }

        // Remove any spaces or special characters
        String cleanedNumber = phoneNumber.replaceAll("[^0-9]", "");

        // Handle different formats
        if (cleanedNumber.startsWith("254")) {
            // Already in international format
            if (cleanedNumber.length() != 12) {
                throw new BusinessException(
                        ResponseStatusEnum.BAD_REQUEST,
                        "Invalid phone number format",
                        "Phone number in 254 format must be 12 digits"
                );
            }
            return cleanedNumber;
        } else if (cleanedNumber.startsWith("0")) {
            // Convert from 07XXXXXXXX to 2547XXXXXXXX
            if (cleanedNumber.length() != 10) {
                throw new BusinessException(
                        ResponseStatusEnum.BAD_REQUEST,
                        "Invalid phone number format",
                        "Phone number in 0 format must be 10 digits"
                );
            }
            return "254" + cleanedNumber.substring(1);
        } else if (cleanedNumber.length() == 9) {
            // Handle 7XXXXXXXX format
            return "254" + cleanedNumber;
        } else {
            throw new BusinessException(
                    ResponseStatusEnum.BAD_REQUEST,
                    "Invalid phone number format",
                    "Phone number must be in format 0741819799, 254741819799, or 741819799"
            );
        }
    }

    @Override
    public PaymentResponse initiateSTKPush(PaymentRequest request) {
        try {
            log.info("Starting STK push process for phone: {}, amount: {}", request.getPhoneNumber(), request.getAmount());

            // Validate request
            validatePaymentRequest(request);

            // Format phone number to international format
            String formattedPhoneNumber = formatPhoneNumber(request.getPhoneNumber());
            log.info("Formatted phone number from {} to {}", request.getPhoneNumber(), formattedPhoneNumber);

            // Get access token from M-Pesa
            String accessToken = getMpesaAccessToken();
            log.debug("Successfully obtained M-Pesa access token: {}",
                    accessToken.substring(0, Math.min(20, accessToken.length())) + "...");

            // Generate password and timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String password = generatePassword(timestamp);

            // Create STK push request with formatted phone number
            StkPushRequest stkRequest = createStkPushRequest(request, password, timestamp, formattedPhoneNumber);

            // Set headers with proper authorization
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Cache-Control", "no-cache");

            HttpEntity<StkPushRequest> entity = new HttpEntity<>(stkRequest, headers);

            // Make API call using M-Pesa RestTemplate
            String url = mpesaProperties.getStkPushRequestUrl();
            log.debug("Making STK push request to: {}", url);
            log.debug("Request headers: Authorization: Bearer {}",
                    accessToken.substring(0, Math.min(20, accessToken.length())) + "...");
            log.debug("Request body: {}", stkRequest);

            ResponseEntity<StkPushResponse> response = mpesaRestTemplate.exchange(
                    url, HttpMethod.POST, entity, StkPushResponse.class);

            StkPushResponse stkResponse = response.getBody();

            if (stkResponse == null) {
                throw new BusinessException(
                        ResponseStatusEnum.ERROR,
                        "Empty response received from M-Pesa API",
                        "M-Pesa API returned null response"
                );
            }

            log.info("STK push response - ResponseCode: {}, CheckoutRequestID: {}, Description: {}",
                    stkResponse.getResponseCode(), stkResponse.getCheckoutRequestID(),
                    stkResponse.getResponseDescription());

            // Check if STK push was successful
            if (!"0".equals(stkResponse.getResponseCode())) {
                log.warn("STK push failed with response code: {}, message: {}, description: {}",
                        stkResponse.getResponseCode(), stkResponse.getCustomerMessage(),
                        stkResponse.getResponseDescription());

                throw new BusinessException(
                        ResponseStatusEnum.BAD_REQUEST,
                        "STK push failed: " + stkResponse.getCustomerMessage(),
                        "M-Pesa response code: " + stkResponse.getResponseCode() +
                                ", Description: " + stkResponse.getResponseDescription()
                );
            }

            // Save transaction to database with original phone number from request
            PaymentTransaction transaction = createPaymentTransaction(request, stkResponse);
            PaymentTransaction savedTransaction = paymentRepository.save(transaction);
            log.info("Payment transaction saved with ID: {}", savedTransaction.getId());

            // Return successful response
            return PaymentResponse.builder()
                    .merchantRequestId(stkResponse.getMerchantRequestID())
                    .checkoutRequestId(stkResponse.getCheckoutRequestID())
                    .responseCode(stkResponse.getResponseCode())
                    .message(stkResponse.getCustomerMessage())
                    .status(PaymentStatus.PENDING.name())
                    .build();

        } catch (HttpClientErrorException e) {
            log.error("Client error during STK push - Status: {}, Headers: {}, Response: {}",
                    e.getStatusCode(), e.getResponseHeaders(), e.getResponseBodyAsString());
            throw new BusinessException(
                    ResponseStatusEnum.BAD_REQUEST,
                    "Invalid request to M-Pesa API: " + e.getStatusCode(),
                    "HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString()
            );

        } catch (HttpServerErrorException e) {
            log.error("Server error during STK push - Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(
                    ResponseStatusEnum.ERROR,
                    "M-Pesa API server error",
                    "HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString()
            );

        } catch (Exception e) {
            log.error("Unexpected error during STK push for phone: {}", request.getPhoneNumber(), e);
            throw new BusinessException(
                    ResponseStatusEnum.ERROR,
                    "Failed to initiate payment",
                    e.getMessage()
            );
        }
    }

    @Override
    public void handleCallback(StkCallback callback) {
        try {
            if (callback == null || callback.getBody() == null || callback.getBody().getStkCallback() == null) {
                log.warn("Received null or invalid callback data");
                throw new BusinessException(
                        ResponseStatusEnum.BAD_REQUEST,
                        "Invalid callback data received",
                        "Callback data is null or malformed"
                );
            }

            StkCallback.StkCallbackData callbackData = callback.getBody().getStkCallback();
            String checkoutRequestId = callbackData.getCheckoutRequestID();
            Integer resultCode = callbackData.getResultCode();

            log.info("Processing callback for CheckoutRequestID: {}, ResultCode: {}, ResultDesc: {}",
                    checkoutRequestId, resultCode, callbackData.getResultDesc());

            Optional<PaymentTransaction> transactionOpt = paymentRepository.findByCheckoutRequestId(checkoutRequestId);

            if (transactionOpt.isEmpty()) {
                log.warn("No transaction found for CheckoutRequestID: {}", checkoutRequestId);
                throw new BusinessException(
                        ResponseStatusEnum.NOT_FOUND,
                        "Transaction not found",
                        "No transaction found for CheckoutRequestID: " + checkoutRequestId
                );
            }

            PaymentTransaction transaction = transactionOpt.get();

            // Map M-Pesa result codes to payment statuses
            PaymentStatus newStatus = mapResultCodeToStatus(resultCode, callbackData.getResultDesc());
            transaction.setStatus(newStatus);

            log.info("Payment status updated to {} for CheckoutRequestID: {}", newStatus, checkoutRequestId);

            // Extract additional data for successful payments
            if (resultCode == 0) {
                extractCallbackMetadata(callbackData, transaction);
            }

            transaction.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(transaction);

            log.info("Payment callback processed successfully for CheckoutRequestID: {} with status: {}",
                    checkoutRequestId, newStatus);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing payment callback", e);
            throw new BusinessException(
                    ResponseStatusEnum.ERROR,
                    "Failed to process payment callback",
                    e.getMessage()
            );
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
                    "No transaction found for CheckoutRequestID: " + checkoutRequestId
            );
        }

        PaymentTransaction transaction = transactionOpt.get();

        return PaymentStatusResponse.builder()
                .checkoutRequestId(transaction.getCheckoutRequestId())
                .merchantRequestId(transaction.getMerchantRequestId())
                .phoneNumber(transaction.getPhoneNumber())
                .amount(transaction.getAmount())
                .accountReference(transaction.getAccountReference())
                .status(transaction.getStatus().name())
                .mpesaReceiptNumber(transaction.getMpesaReceiptNumber())
                .transactionDate(transaction.getTransactionDate())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }

    private void validatePaymentRequest(PaymentRequest request) {
        if (request == null) {
            throw new BusinessException(
                    ResponseStatusEnum.BAD_REQUEST,
                    "Payment request cannot be null",
                    "Request body is required"
            );
        }
        if (!StringUtils.hasText(request.getPhoneNumber())) {
            throw new BusinessException(
                    ResponseStatusEnum.BAD_REQUEST,
                    "Phone number is required",
                    "phoneNumber field cannot be empty"
            );
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    ResponseStatusEnum.BAD_REQUEST,
                    "Valid amount is required",
                    "amount must be greater than 0"
            );
        }
        if (!StringUtils.hasText(request.getAccountReference())) {
            throw new BusinessException(
                    ResponseStatusEnum.BAD_REQUEST,
                    "Account reference is required",
                    "accountReference field cannot be empty"
            );
        }
    }

    private PaymentTransaction createPaymentTransaction(PaymentRequest request, StkPushResponse stkResponse) {
        return PaymentTransaction.builder()
                .merchantRequestId(stkResponse.getMerchantRequestID())
                .checkoutRequestId(stkResponse.getCheckoutRequestID())
                .phoneNumber(request.getPhoneNumber()) // Store original format for display
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

            log.info("Consumer Key: {}", mpesaProperties.getConsumerKey());
            log.info("Consumer Secret: {}", mpesaProperties.getConsumerSecret() != null ?
                    mpesaProperties.getConsumerSecret().substring(0, Math.min(10, mpesaProperties.getConsumerSecret().length())) + "..." : "NULL");
            log.info("Encoded Auth: {}", encodedAuth.substring(0, Math.min(20, encodedAuth.length())) + "...");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = mpesaProperties.getOauthEndpoint() + "?grant_type=client_credentials";
            log.info("OAuth URL: {}", url);

            ResponseEntity<AccessTokenResponse> response = mpesaRestTemplate.exchange(
                    url, HttpMethod.GET, entity, AccessTokenResponse.class);

            if (response.getBody() == null || response.getBody().getAccessToken() == null) {
                log.error("M-Pesa API returned null access token. Status: {}, Headers: {}",
                        response.getStatusCode(), response.getHeaders());
                throw new BusinessException(
                        ResponseStatusEnum.ERROR,
                        "Failed to obtain access token from M-Pesa API",
                        "M-Pesa API returned null access token"
                );
            }

            String accessToken = response.getBody().getAccessToken();
            log.info("Successfully obtained M-Pesa access token: {}",
                    accessToken.substring(0, Math.min(20, accessToken.length())) + "...");
            log.info("Token expires in: {} seconds", response.getBody().getExpiresIn());

            return accessToken;

        } catch (HttpClientErrorException e) {
            log.error("HTTP Client Error getting access token - Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(
                    ResponseStatusEnum.BAD_REQUEST,
                    "Failed to authenticate with M-Pesa API: " + e.getStatusCode(),
                    e.getResponseBodyAsString()
            );
        } catch (HttpServerErrorException e) {
            log.error("HTTP Server Error getting access token - Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(
                    ResponseStatusEnum.ERROR,
                    "M-Pesa API server error",
                    "HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString()
            );
        } catch (Exception e) {
            log.error("Unexpected error getting M-Pesa access token", e);
            throw new BusinessException(
                    ResponseStatusEnum.ERROR,
                    "Failed to authenticate with M-Pesa API",
                    e.getMessage()
            );
        }
    }

    private String generatePassword(String timestamp) {
        try {
            String rawPassword = mpesaProperties.getBusinessShortCode() + mpesaProperties.getPassKey() + timestamp;
            return Base64.getEncoder().encodeToString(rawPassword.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to generate password", e);
            throw new BusinessException(
                    ResponseStatusEnum.ERROR,
                    "Failed to generate authentication password",
                    e.getMessage()
            );
        }
    }

    private StkPushRequest createStkPushRequest(PaymentRequest request, String password, String timestamp, String formattedPhoneNumber) {
        return StkPushRequest.builder()
                .businessShortCode(mpesaProperties.getBusinessShortCode())
                .password(password)
                .timestamp(timestamp)
                .transactionType("CustomerPayBillOnline")
                .amount(request.getAmount())
                .partyA(formattedPhoneNumber) // Use formatted international number
                .partyB(mpesaProperties.getBusinessShortCode())
                .phoneNumber(formattedPhoneNumber) // Use formatted international number
                .callBackURL(mpesaProperties.getCallbackUrl())
                .accountReference(request.getAccountReference())
                .transactionDesc(request.getTransactionDescription())
                .build();
    }

    private PaymentStatus mapResultCodeToStatus(Integer resultCode, String resultDesc) {
        if (resultCode == null) {
            return PaymentStatus.FAILED;
        }

        switch (resultCode) {
            case 0:
                return PaymentStatus.COMPLETED;
            case 1:
                return PaymentStatus.FAILED; // Insufficient funds
            case 1001:
                return PaymentStatus.FAILED; // Invalid phone number
            case 1019:
                return PaymentStatus.FAILED; // Invalid amount
            case 1032:
                return PaymentStatus.CANCELLED; // Request cancelled by user
            case 1037:
                return PaymentStatus.EXPIRED; // DS timeout user cannot be reached
            case 2001:
                return PaymentStatus.FAILED; // Wrong PIN entered
            case 1025:
                return PaymentStatus.FAILED; // Unable to lock subscriber amount
            case 1026:
                return PaymentStatus.FAILED; // Subscriber not active
            case 1027:
                return PaymentStatus.FAILED; // Not a registered M-PESA user
            case 1036:
                return PaymentStatus.CANCELLED; // Transaction cancelled by user
            case 1012:
                return PaymentStatus.EXPIRED; // Transaction expired
            case 9999:
                return PaymentStatus.FAILED; // Request timeout
            default:
                log.warn("Unknown M-Pesa result code: {} with description: {}", resultCode, resultDesc);
                return PaymentStatus.FAILED;
        }
    }

    private void extractCallbackMetadata(StkCallback.StkCallbackData callbackData, PaymentTransaction transaction) {
        if (callbackData.getCallbackMetadata() != null && callbackData.getCallbackMetadata().getItem() != null) {
            for (StkCallback.CallbackItem item : callbackData.getCallbackMetadata().getItem()) {
                switch (item.getName()) {
                    case "MpesaReceiptNumber":
                        if (item.getValue() != null) {
                            transaction.setMpesaReceiptNumber(item.getValue().toString());
                            log.info("M-Pesa receipt number extracted: {}", transaction.getMpesaReceiptNumber());
                        }
                        break;
                    case "TransactionDate":
                        // You can extract and store transaction date if needed
                        log.debug("Transaction date from M-Pesa: {}", item.getValue());
                        break;
                    case "Amount":
                        // Verify amount matches
                        log.debug("Amount from M-Pesa: {}", item.getValue());
                        break;
                    case "PhoneNumber":
                        // Verify phone number matches
                        log.debug("Phone number from M-Pesa: {}", item.getValue());
                        break;
                }
            }
        }
    }
}