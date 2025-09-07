package com.paymentservice.paymentservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentResponse {
    private String merchantRequestId;
    private String checkoutRequestId;
    private String responseCode;
    private String message;
    private String status;
}
