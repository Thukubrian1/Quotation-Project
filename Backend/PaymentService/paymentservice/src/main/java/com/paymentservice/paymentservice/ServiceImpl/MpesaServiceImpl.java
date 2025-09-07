package com.paymentservice.paymentservice.ServiceImpl;

import com.paymentservice.paymentservice.Config.MpesaProperties;
import com.paymentservice.paymentservice.DTOs.*;
import com.paymentservice.paymentservice.Entity.PaymentTransaction;
import com.paymentservice.paymentservice.Repository.PaymentTransactionRepository;
import com.paymentservice.paymentservice.Service.MpesaService;
import com.shared.sharedlib.Enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
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

    private final MpesaProperties mpesaProperties;
    private final PaymentTransactionRepository paymentRepository;
    private final RestTemplate restTemplate;

    @Override
    public PaymentResponse initiateSTKPush(PaymentRequest request) {
        try {
            log.info("Starting STK push process for phone: {}, amount: {}", request.getPhoneNumber(), request.getAmount());

            // Validate request
            validatePaymentRequest(request);

            // Get access token
            String accessToken = getAccessToken();
            log.debug("Successfully obtained access token");

            // Generate password and timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String password = generatePassword(timestamp);

            // Create STK push request
            StkPushRequest stkRequest = createStkPushRequest(request, password, timestamp);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<StkPushRequest> entity = new HttpEntity<>(stkRequest, headers);

            // Make API call
            String url = mpesaProperties.getBaseUrl() + "/mpesa/stkpush/v1/processrequest";
            log.debug("Making STK push request to: {}", url);

            ResponseEntity<StkPushResponse> response = restTemplate.postForEntity(url, entity, StkPushResponse.class);
            StkPushResponse stkResponse = response.getBody();

            if (stkResponse == null) {
                throw new RuntimeException("Empty response received from M-Pesa API");
            }

            log.info("STK push response - ResponseCode: {}, CheckoutRequestID: {}",
                    stkResponse.getResponseCode(), stkResponse.getCheckoutRequestID());

            // Check if STK push was successful
            if (!"0".equals(stkResponse.getResponseCode())) {
                log.warn("STK push failed with response code: {}, message: {}",
                        stkResponse.getResponseCode(), stkResponse.getResponseDescription());

                return PaymentResponse.builder()
                        .merchantRequestId(stkResponse.getMerchantRequestID())
                        .checkoutRequestId(stkResponse.getCheckoutRequestID())
                        .responseCode(stkResponse.getResponseCode())
                        .message(stkResponse.getCustomerMessage())
                        .status(PaymentStatus.FAILED.name())
                        .build();
            }

            // Save transaction to database
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
            log.error("Client error during STK push - Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Invalid request to M-Pesa API: " + e.getMessage(), e);

        } catch (HttpServerErrorException e) {
            log.error("Server error during STK push - Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("M-Pesa API server error: " + e.getMessage(), e);

        } catch (ResourceAccessException e) {
            log.error("Network error during STK push", e);
            throw new RuntimeException("Network error connecting to M-Pesa API: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Unexpected error during STK push for phone: {}", request.getPhoneNumber(), e);
            throw new RuntimeException("Failed to initiate payment: " + e.getMessage(), e);
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

            log.info("Processing callback for CheckoutRequestID: {}, ResultCode: {}",
                    checkoutRequestId, callbackData.getResultCode());

            Optional<PaymentTransaction> transactionOpt = paymentRepository.findByCheckoutRequestId(checkoutRequestId);

            if (transactionOpt.isEmpty()) {
                log.warn("No transaction found for CheckoutRequestID: {}", checkoutRequestId);
                return;
            }

            PaymentTransaction transaction = transactionOpt.get();

            if (callbackData.getResultCode() == 0) {
                // Payment successful
                transaction.setStatus(PaymentStatus.COMPLETED);
                log.info("Payment successful for CheckoutRequestID: {}", checkoutRequestId);

                // Extract M-Pesa receipt number
                extractReceiptNumber(callbackData, transaction);

            } else {
                // Payment failed
                transaction.setStatus(PaymentStatus.FAILED);
                log.warn("Payment failed for CheckoutRequestID: {}, ResultCode: {}, ResultDesc: {}",
                        checkoutRequestId, callbackData.getResultCode(), callbackData.getResultDesc());
            }

            transaction.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(transaction);

            log.info("Payment callback processed successfully for CheckoutRequestID: {}", checkoutRequestId);

        } catch (Exception e) {
            log.error("Error processing payment callback", e);
            throw new RuntimeException("Failed to process payment callback: " + e.getMessage(), e);
        }
    }

    private void validatePaymentRequest(PaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Payment request cannot be null");
        }
        if (!StringUtils.hasText(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number is required");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valid amount is required");
        }
        if (!StringUtils.hasText(request.getAccountReference())) {
            throw new IllegalArgumentException("Account reference is required");
        }

        // Validate phone number format (basic validation)
        String phoneNumber = request.getPhoneNumber().trim();
        if (!phoneNumber.matches("^254\\d{9}$")) {
            throw new IllegalArgumentException("Phone number must be in format 254XXXXXXXXX");
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

    private void extractReceiptNumber(StkCallback.StkCallbackData callbackData, PaymentTransaction transaction) {
        if (callbackData.getCallbackMetadata() != null && callbackData.getCallbackMetadata().getItem() != null) {
            for (StkCallback.CallbackItem item : callbackData.getCallbackMetadata().getItem()) {
                if ("MpesaReceiptNumber".equals(item.getName()) && item.getValue() != null) {
                    transaction.setMpesaReceiptNumber(item.getValue().toString());
                    log.info("M-Pesa receipt number extracted: {}", transaction.getMpesaReceiptNumber());
                    break;
                }
            }
        }
    }

    private String getAccessToken() {
        try {
            String auth = mpesaProperties.getConsumerKey() + ":" + mpesaProperties.getConsumerSecret();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = mpesaProperties.getBaseUrl() + "/oauth/v1/generate?grant_type=client_credentials";
            ResponseEntity<AccessTokenResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, AccessTokenResponse.class);

            if (response.getBody() == null || response.getBody().getAccessToken() == null) {
                throw new RuntimeException("Failed to obtain access token from M-Pesa API");
            }

            return response.getBody().getAccessToken();

        } catch (Exception e) {
            log.error("Failed to get M-Pesa access token", e);
            throw new RuntimeException("Failed to authenticate with M-Pesa API: " + e.getMessage(), e);
        }
    }

    private String generatePassword(String timestamp) {
        try {
            String rawPassword = mpesaProperties.getBusinessShortCode() + mpesaProperties.getPasskey() + timestamp;
            return Base64.getEncoder().encodeToString(rawPassword.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to generate password", e);
            throw new RuntimeException("Failed to generate authentication password", e);
        }
    }

    private StkPushRequest createStkPushRequest(PaymentRequest request, String password, String timestamp) {
        return StkPushRequest.builder()
                .businessShortCode(mpesaProperties.getBusinessShortCode())
                .password(password)
                .timestamp(timestamp)
                .transactionType("CustomerPayBillOnline")
                .amount(request.getAmount())
                .partyA(request.getPhoneNumber())
                .partyB(mpesaProperties.getBusinessShortCode())
                .phoneNumber(request.getPhoneNumber())
                .callBackURL(mpesaProperties.getCallbackUrl())
                .accountReference(request.getAccountReference())
                .transactionDesc(request.getTransactionDescription())
                .build();
    }
}