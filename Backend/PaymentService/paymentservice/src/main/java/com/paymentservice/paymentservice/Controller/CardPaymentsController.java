package com.paymentservice.paymentservice.Controller;

import com.paymentservice.paymentservice.DTOs.CardPaymentRequestDTO;
import com.paymentservice.paymentservice.DTOs.CardPaymentResponseDTO;
import com.paymentservice.paymentservice.Service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor

public class CardPaymentsController {

    private final TransactionService transactionService;

    @PostMapping("/charge")
    public ResponseEntity<CardPaymentResponseDTO> processCardPayment(@Valid @RequestBody CardPaymentRequestDTO request) {
        try {
            CardPaymentResponseDTO response = transactionService.processCardPayment(request);

            if ("COMPLETED".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else if (response.getStatus().startsWith("VALIDATION")) {
                return ResponseEntity.badRequest().body(response);
            } else {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
            }
        } catch (Exception e) {
            CardPaymentResponseDTO errorResponse = new CardPaymentResponseDTO();
            errorResponse.setStatus("SYSTEM_ERROR");
            errorResponse.setBankResponseMessage("An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/refund/{transactionId}")
    public ResponseEntity<CardPaymentResponseDTO> refundPayment(
            @PathVariable String transactionId,
            @RequestBody(required = false) Map<String, String> refundRequest) {

        try {
            String reason = refundRequest != null ? refundRequest.get("reason") : "Merchant refund";
            CardPaymentResponseDTO response = transactionService.refundTransaction(transactionId, reason);

            if ("REFUNDED".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            CardPaymentResponseDTO errorResponse = new CardPaymentResponseDTO();
            errorResponse.setStatus("SYSTEM_ERROR");
            errorResponse.setBankResponseMessage("Refund processing failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/card/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Payment Processing API");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(response);
    }
}