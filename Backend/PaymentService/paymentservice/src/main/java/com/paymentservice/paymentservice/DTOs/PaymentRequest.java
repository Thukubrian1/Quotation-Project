package com.paymentservice.paymentservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    private String phoneNumber;
    private String amount;
    private String accountReference;
    private String transactionDescription;
}
