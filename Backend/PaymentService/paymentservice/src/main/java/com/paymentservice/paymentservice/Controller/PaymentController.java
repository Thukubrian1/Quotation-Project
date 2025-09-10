package com.paymentservice.paymentservice.Controller;

import com.paymentservice.paymentservice.Config.MpesaProperties;
import com.paymentservice.paymentservice.DTOs.AccessTokenResponse;
import com.paymentservice.paymentservice.DTOs.PaymentRequest;
import com.paymentservice.paymentservice.DTOs.PaymentResponse;
import com.paymentservice.paymentservice.DTOs.StkCallback;
import com.paymentservice.paymentservice.Service.MpesaService;
import com.shared.sharedlib.Dtos.GenericResponse;
import com.shared.sharedlib.Enums.ResponseStatusEnum;
import com.shared.sharedlib.Exceptions.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final MpesaService mpesaService;
    private final MpesaProperties mpesaProperties;

    @PostMapping("/mpesa/stk-push")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<GenericResponse<PaymentResponse>> initiateMpesaPayment(@Valid @RequestBody PaymentRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            log.info("Initiating M-Pesa STK push for user: {}, phone number: {}", username, request.getPhoneNumber());

            PaymentResponse response = mpesaService.initiateSTKPush(request);

            GenericResponse<PaymentResponse> genericResponse = GenericResponse.<PaymentResponse>builder()
                    .status(ResponseStatusEnum.SUCCESS)
                    .message("STK push initiated successfully")
                    .data(response)
                    .build();

            return ResponseEntity.ok(genericResponse);

        } catch (BusinessException e) {
            log.error("Business error during STK push initiation", e);

            GenericResponse<PaymentResponse> errorResponse = GenericResponse.<PaymentResponse>builder()
                    .status(e.getStatus())
                    .message(e.getMessage())
                    .debugMessage(e.getDebugMessage())
                    .build();

            return ResponseEntity.status(e.getStatus().getHttpStatus()).body(errorResponse);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request data for STK push", e);

            GenericResponse<PaymentResponse> errorResponse = GenericResponse.<PaymentResponse>builder()
                    .status(ResponseStatusEnum.BAD_REQUEST)
                    .message("Invalid request data")
                    .debugMessage(e.getMessage())
                    .build();

            return ResponseEntity.status(ResponseStatusEnum.BAD_REQUEST.getHttpStatus()).body(errorResponse);

        } catch (Exception e) {
            log.error("Unexpected error during STK push initiation", e);

            GenericResponse<PaymentResponse> errorResponse = GenericResponse.<PaymentResponse>builder()
                    .status(ResponseStatusEnum.ERROR)
                    .message("Failed to initiate payment")
                    .debugMessage(e.getMessage())
                    .build();

            return ResponseEntity.status(ResponseStatusEnum.ERROR.getHttpStatus()).body(errorResponse);
        }
    }

    @PostMapping("/mpesa/callback")
    public ResponseEntity<GenericResponse<String>> handleMpesaCallback(@RequestBody StkCallback callback) {
        try {
            log.info("Received M-Pesa callback for CheckoutRequestID: {}",
                    callback.getBody() != null && callback.getBody().getStkCallback() != null ?
                            callback.getBody().getStkCallback().getCheckoutRequestID() : "Unknown");

            mpesaService.handleCallback(callback);

            GenericResponse<String> response = GenericResponse.<String>builder()
                    .status(ResponseStatusEnum.SUCCESS)
                    .message("Callback processed successfully")
                    .data("Callback processed")
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing M-Pesa callback", e);

            GenericResponse<String> response = GenericResponse.<String>builder()
                    .status(ResponseStatusEnum.ERROR)
                    .message("Failed to process callback")
                    .debugMessage(e.getMessage())
                    .build();

            return ResponseEntity.status(ResponseStatusEnum.ERROR.getHttpStatus()).body(response);
        }
    }

    @GetMapping("/test-oauth")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<GenericResponse<String>> testOAuth() {
        try {

            RestTemplate mpesaRestTemplate = new RestTemplate();

            String auth = mpesaProperties.getConsumerKey() + ":" + mpesaProperties.getConsumerSecret();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + encodedAuth);
            headers.set("Cache-Control", "no-cache");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = mpesaProperties.getOauthEndpoint() + "?grant_type=client_credentials";

            log.info("Testing OAuth with URL: {}", url);
            log.info("Consumer Key: {}", mpesaProperties.getConsumerKey());
            log.info("Encoded Auth (first 20 chars): {}", encodedAuth.substring(0, Math.min(20, encodedAuth.length())) + "...");

            ResponseEntity<AccessTokenResponse> response = mpesaRestTemplate.exchange(
                    url, HttpMethod.GET, entity, AccessTokenResponse.class);

            AccessTokenResponse tokenResponse = response.getBody();
            if (tokenResponse != null && tokenResponse.getAccessToken() != null) {
                String message = "OAuth successful! Token: " +
                        tokenResponse.getAccessToken().substring(0, Math.min(20, tokenResponse.getAccessToken().length())) +
                        "... Expires in: " + tokenResponse.getExpiresIn() + " seconds";

                GenericResponse<String> successResponse = GenericResponse.success(message);
                return ResponseEntity.ok(successResponse);
            } else {
                GenericResponse<String> errorResponse = GenericResponse.<String>builder()
                        .status(ResponseStatusEnum.ERROR)
                        .message("OAuth failed - null response")
                        .build();
                return ResponseEntity.status(500).body(errorResponse);
            }

        } catch (HttpClientErrorException e) {
            log.error("OAuth test failed - Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());

            GenericResponse<String> errorResponse = GenericResponse.<String>builder()
                    .status(ResponseStatusEnum.BAD_REQUEST)
                    .message("OAuth failed: " + e.getStatusCode())
                    .debugMessage("Response: " + e.getResponseBodyAsString())
                    .build();

            return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
        } catch (Exception e) {
            log.error("OAuth test error", e);

            GenericResponse<String> errorResponse = GenericResponse.<String>builder()
                    .status(ResponseStatusEnum.ERROR)
                    .message("OAuth test failed")
                    .debugMessage(e.getMessage())
                    .build();

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/health")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<GenericResponse<String>> healthCheck() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "anonymous";

        GenericResponse<String> response = GenericResponse.success(
                "Payment service is running. Authenticated user: " + username
        );
        return ResponseEntity.ok(response);
    }

    // Public health check endpoint (no auth required)
    @GetMapping("/public-health")
    public ResponseEntity<GenericResponse<String>> publicHealthCheck() {
        GenericResponse<String> response = GenericResponse.success("Payment service is running");
        return ResponseEntity.ok(response);
    }
}