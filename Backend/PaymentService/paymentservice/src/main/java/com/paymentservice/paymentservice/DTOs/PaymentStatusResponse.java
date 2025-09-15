package com.paymentservice.paymentservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentStatusResponse {

        private String checkoutRequestId;
        private String merchantRequestId;
        private String phoneNumber;
        private BigDecimal amount;
        private String accountReference;
        private String status;
        private String mpesaReceiptNumber;
        private LocalDateTime transactionDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
