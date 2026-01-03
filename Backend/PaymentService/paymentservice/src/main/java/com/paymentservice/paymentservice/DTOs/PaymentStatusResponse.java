package com.paymentservice.paymentservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentStatusResponse {
    private String checkoutRequestId;
    private String merchantRequestId;
    private String phoneNumber;
    private BigDecimal amount;
    private String accountReference;
    private String status;
    private String statusMessage;
    private String mpesaReceiptNumber;
    private LocalDateTime transactionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}