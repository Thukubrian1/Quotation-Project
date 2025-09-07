package com.paymentservice.paymentservice.ServiceImpl;

import com.paymentservice.paymentservice.Config.MpesaProperties;
import com.paymentservice.paymentservice.DTOs.*;
import com.paymentservice.paymentservice.Entity.PaymentTransaction;
import com.paymentservice.paymentservice.Repository.PaymentTransactionRepository;
import com.paymentservice.paymentservice.Service.MpesaService;
import com.shared.sharedlib.Dtos.GenericResponse;
import com.shared.sharedlib.Enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
@Slf4j
@RequiredArgsConstructor
public class MpesaServiceImpl implements MpesaService {

    private final MpesaProperties mpesaProperties;
    private final PaymentTransactionRepository paymentRepository;
    private final RestTemplate restTemplate;

    public GenericResponse<PaymentResponse>  initiateSTKPush(PaymentRequest request) {
            try {
                // Get access token
                String accessToken = getAccessToken();

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
                ResponseEntity<StkPushResponse> response = restTemplate.postForEntity(url, entity, StkPushResponse.class);

                StkPushResponse stkResponse = response.getBody();

                // Save transaction to database
                PaymentTransaction transaction = new PaymentTransaction();
                transaction.setMerchantRequestId(stkResponse.getMerchantRequestID());
                transaction.setCheckoutRequestId(stkResponse.getCheckoutRequestID());
                transaction.setPhoneNumber(request.getPhoneNumber());
                transaction.setAmount(request.getAmount());
                transaction.setAccountReference(request.getAccountReference());
                transaction.setTransactionDescription(request.getTransactionDescription());
                transaction.setStatus(PaymentStatus.PENDING);
                transaction.setTransactionDate(LocalDateTime.now());

                paymentRepository.save(transaction);

                // Return response
                return new PaymentResponse(
                        stkResponse.getMerchantRequestID(),
                        stkResponse.getCheckoutRequestID(),
                        stkResponse.getResponseCode(),
                        stkResponse.getCustomerMessage(),
                        "PENDING"
                );

            } catch (Exception e) {
                log.error("Error initiating STK push: ", e);
                return new PaymentResponse(null, null, "1", "Payment initiation failed", "FAILED");
            }
        }

    public void handleCallback(StkCallback callback){
            try {
                StkCallback.StkCallbackData callbackData = callback.getBody().getStkCallback();
                String checkoutRequestId = callbackData.getCheckoutRequestID();

                PaymentTransaction transaction = paymentRepository.findByCheckoutRequestId(checkoutRequestId)
                        .orElse(null);

                if (transaction != null) {
                    if (callbackData.getResultCode() == 0) {
                        // Payment successful
                        transaction.setStatus(PaymentStatus.COMPLETED);

                        // Extract M-Pesa receipt number
                        if (callbackData.getCallbackMetadata() != null) {
                            for (StkCallback.CallbackItem item : callbackData.getCallbackMetadata().getItem()) {
                                if ("MpesaReceiptNumber".equals(item.getName())) {
                                    transaction.setMpesaReceiptNumber(item.getValue().toString());
                                    break;
                                }
                            }
                        }
                    } else {
                        // Payment failed
                        transaction.setStatus(PaymentStatus.FAILED);
                    }

                    paymentRepository.save(transaction);
                    log.info("Payment callback processed for checkout request ID: {}", checkoutRequestId);
                }

            } catch (Exception e) {
                log.error("Error processing payment callback: ", e);
            }
        }

        private String getAccessToken() {
            String auth = mpesaProperties.getConsumerKey() + ":" + mpesaProperties.getConsumerSecret();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = mpesaProperties.getBaseUrl() + "/oauth/v1/generate?grant_type=client_credentials";
            ResponseEntity<AccessTokenResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, AccessTokenResponse.class);

            return response.getBody().getAccessToken();
        }

        private String generatePassword(String timestamp) {
            String rawPassword = mpesaProperties.getBusinessShortCode() + mpesaProperties.getPasskey() + timestamp;
            return Base64.getEncoder().encodeToString(rawPassword.getBytes(StandardCharsets.UTF_8));
        }

        private StkPushRequest createStkPushRequest(PaymentRequest request, String password, String timestamp) {
            return new StkPushRequest(
                    mpesaProperties.getBusinessShortCode(),
                    password,
                    timestamp,
                    "CustomerPayBillOnline",
                    request.getAmount(),
                    request.getPhoneNumber(),
                    mpesaProperties.getBusinessShortCode(),
                    request.getPhoneNumber(),
                    mpesaProperties.getCallbackUrl(),
                    request.getAccountReference(),
                    request.getTransactionDescription()
            );
        }
}
