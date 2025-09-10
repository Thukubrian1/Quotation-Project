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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    /**
     * Example method showing how to call external service with bearer token
     */
    public void callExternalServiceWithAuth(String data, String username, String password) {
        try {
            // Option 1: Use the pre-configured RestTemplate with interceptor (if token is set in config)
            if (isExternalTokenConfigured()) {
                callExternalServiceWithConfiguredToken(data);
            } else {
                // Option 2: Get token dynamically from auth service
                callExternalServiceWithDynamicToken(data, username, password);
            }

        } catch (Exception e) {
            log.error("Error calling external service", e);
            throw new BusinessException(
                    ResponseStatusEnum.ERROR,
                    "Failed to communicate with external service",
                    e.getMessage()
            );
        }
    }

    private void callExternalServiceWithConfiguredToken(String data) {
        String url = authServiceBaseUrl + "/some-endpoint";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(data, headers);

        // This will automatically include the bearer token via interceptor
        ResponseEntity<String> response = authenticatedRestTemplate.postForEntity(
                url, entity, String.class
        );

        log.info("External service response: {}", response.getBody());
    }

    private void callExternalServiceWithDynamicToken(String data, String username, String password) {
        // Get token from auth service
        String token = authServiceClient.login(username, password);

        String url = authServiceBaseUrl + "/some-endpoint";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<String> entity = new HttpEntity<>(data, headers);

        // Use regular RestTemplate with explicit Authorization header
        ResponseEntity<String> response = mpesaRestTemplate.postForEntity(
                url, entity, String.class
        );

        log.info("External service response: {}", response.getBody());
    }

    /**
     * Example: Call external service using token from current security context
     */
    public void callExternalServiceWithCurrentUserToken(String data) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new BusinessException(
                        ResponseStatusEnum.UNAUTHORIZED,
                        "User not authenticated",
                        "No valid authentication context found"
                );
            }

            String currentUser = auth.getName();
            log.info("Making external service call for authenticated user: {}", currentUser);

            String url = authServiceBaseUrl + "/some-endpoint";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // Note: In a real scenario, you'd extract the actual JWT token from the request
            // For now, using the configured bearer token

            HttpEntity<String> entity = new HttpEntity<>(data, headers);

            ResponseEntity<String> response = authenticatedRestTemplate.postForEntity(
                    url, entity, String.class
            );

            log.info("External service call successful for user: {}", currentUser);

        } catch (Exception e) {
            log.error("Error calling external service for current user", e);
            throw new BusinessException(
                    ResponseStatusEnum.ERROR,
                    "Failed to communicate with external service",
                    e.getMessage()
            );
        }
    }

    private boolean isExternalTokenConfigured() {
        return StringUtils.hasText(System.getProperty("external.service.bearer-token")) ||
                StringUtils.hasText(System.getenv("EXTERNAL_SERVICE_TOKEN"));
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

            log.info("Processing callback for CheckoutRequestID: {}, ResultCode: {}",
                    checkoutRequestId, callbackData.getResultCode());

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

        } catch (BusinessException e) {
            // Re-throw business exceptions as-is
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
}