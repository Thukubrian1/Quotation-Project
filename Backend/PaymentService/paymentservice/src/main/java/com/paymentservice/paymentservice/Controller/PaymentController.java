package com.paymentservice.paymentservice.Controller;

import com.paymentservice.paymentservice.DTOs.PaymentRequest;
import com.paymentservice.paymentservice.DTOs.PaymentResponse;
import com.paymentservice.paymentservice.DTOs.StkCallback;
import com.paymentservice.paymentservice.Service.MpesaService;
import com.shared.sharedlib.Dtos.GenericResponse;
import com.shared.sharedlib.Enums.ResponseStatusEnum;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final MpesaService mpesaService;

    @PostMapping("/mpesa/stk-push")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PaymentResponse> initiateMpesaPayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Initiating M-Pesa STK push for phone number: {}", request.getPhoneNumber());

        PaymentResponse response = mpesaService.initiateSTKPush(request);

        return ResponseEntity.status(ResponseStatusEnum.SUCCESS.getHttpStatus()).body(response);
    }

    @PostMapping("/mpesa/callback")
    public ResponseEntity<GenericResponse<String>> handleMpesaCallback(@RequestBody StkCallback callback) {
        try {
            log.info("Received M-Pesa callback for CheckoutRequestID: {}",
                    callback.getBody() != null ? callback.getBody().getStkCallback().getCheckoutRequestID() : "Unknown");

            mpesaService.handleCallback(callback);

            GenericResponse<String> response = GenericResponse.<String>builder()
                    .status(ResponseStatusEnum.SUCCESS)
                    .message("Callback processed successfully")
                    .data("Callback processed")
                    .build();

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Failed to process M-Pesa callback", e);

            GenericResponse<String> response = GenericResponse.<String>builder()
                    .status(ResponseStatusEnum.ERROR)
                    .message("Failed to process callback")
                    .debugMessage(e.getMessage())
                    .build();

            return ResponseEntity.status(ResponseStatusEnum.ERROR.getHttpStatus()).body(response);

        } catch (Exception e) {
            log.error("Unexpected error processing M-Pesa callback", e);

            GenericResponse<String> response = GenericResponse.<String>builder()
                    .status(ResponseStatusEnum.ERROR)
                    .message("An unexpected error occurred while processing callback")
                    .debugMessage(e.getMessage())
                    .build();

            return ResponseEntity.status(ResponseStatusEnum.ERROR.getHttpStatus()).body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<GenericResponse<String>> healthCheck() {
        GenericResponse<String> response = GenericResponse.success("Payment service is running");
        return ResponseEntity.ok(response);
    }
}