package com.paymentservice.paymentservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    private String phoneNumber;
    private BigDecimal amount;
    private String accountReference;
    private String transactionDescription;
}
