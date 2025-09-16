package com.paymentservice.paymentservice.ServiceImpl;

import com.paymentservice.paymentservice.Service.CardProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CardProcessingServiceImpl implements CardProcessingService {

    @Value("${bank.api.endpoint}")
    private String bankApiEndpoint;

    @Value("${bank.api.key}")
    private String bankApiKey;

    @Value("${bank.merchant.id}")
    private String bankMerchantId;

    private final RestTemplate restTemplate = new RestTemplate();

    public BankResponse processPayment(String cardNumber, String cvv, String expiryMonth,
                                       String expiryYear, String cardholderName,
                                       BigDecimal amount, String currency, String transactionId) {

        try {
            // Prepare bank API request
            Map<String, Object> bankRequest = new HashMap<>();
            bankRequest.put("merchant_id", bankMerchantId);
            bankRequest.put("transaction_id", transactionId);
            bankRequest.put("amount", amount);
            bankRequest.put("currency", currency);
            bankRequest.put("card_number", cardNumber);
            bankRequest.put("cvv", cvv);
            bankRequest.put("expiry_month", expiryMonth);
            bankRequest.put("expiry_year", expiryYear);
            bankRequest.put("cardholder_name", cardholderName);
            bankRequest.put("transaction_type", "SALE");

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + bankApiKey);
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(bankRequest, headers);

            // Call bank API
            ResponseEntity<Map> response = restTemplate.exchange(
                    bankApiEndpoint + "/transactions/authorize",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();

            return new BankResponse(
                    (String) responseBody.get("transaction_id"),
                    (String) responseBody.get("authorization_code"),
                    (String) responseBody.get("response_code"),
                    (String) responseBody.get("response_message"),
                    "SUCCESS".equals(responseBody.get("status"))
            );

        } catch (Exception e) {
            return new BankResponse(
                    transactionId,
                    null,
                    "ERR_001",
                    "Bank communication error: " + e.getMessage(),
                    false
            );
        }
    }

    public BankResponse refundPayment(String originalTransactionId, BigDecimal amount) {
        try {
            Map<String, Object> refundRequest = new HashMap<>();
            refundRequest.put("merchant_id", bankMerchantId);
            refundRequest.put("original_transaction_id", originalTransactionId);
            refundRequest.put("amount", amount);
            refundRequest.put("transaction_type", "REFUND");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + bankApiKey);
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(refundRequest, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    bankApiEndpoint + "/transactions/refund",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();

            return new BankResponse(
                    (String) responseBody.get("transaction_id"),
                    (String) responseBody.get("authorization_code"),
                    (String) responseBody.get("response_code"),
                    (String) responseBody.get("response_message"),
                    "SUCCESS".equals(responseBody.get("status"))
            );

        } catch (Exception e) {
            return new BankResponse(
                    null,
                    null,
                    "ERR_002",
                    "Refund processing error: " + e.getMessage(),
                    false
            );
        }
    }

    public static class BankResponse {
        private final String transactionId;
        private final String authorizationCode;
        private final String responseCode;
        private final String responseMessage;
        private final boolean success;

        public BankResponse(String transactionId, String authorizationCode,
                            String responseCode, String responseMessage, boolean success) {
            this.transactionId = transactionId;
            this.authorizationCode = authorizationCode;
            this.responseCode = responseCode;
            this.responseMessage = responseMessage;
            this.success = success;
        }

        // Getters
        public String getTransactionId() { return transactionId; }
        public String getAuthorizationCode() { return authorizationCode; }
        public String getResponseCode() { return responseCode; }
        public String getResponseMessage() { return responseMessage; }
        public boolean isSuccess() { return success; }
    }
}
