package com.paymentservice.paymentservice.Controller;

import com.paymentservice.paymentservice.DTOs.PaymentRequest;
import com.paymentservice.paymentservice.DTOs.PaymentResponse;
import com.paymentservice.paymentservice.DTOs.StkCallback;
import com.paymentservice.paymentservice.Service.MpesaService;
import com.shared.sharedlib.Dtos.GenericResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        GenericResponse response = mpesaService.initiateSTKPush(request);

        return ResponseEntity.<GenericResponse>(response);
    }

    @PostMapping("/mpesa/callback")
    public ResponseEntity<String> handleMpesaCallback(@RequestBody StkCallback callback) {
        log.info("Received M-Pesa callback");
        mpesaService.handleCallback(callback);
        return ResponseEntity.ok("Callback processed successfully");
    }
}