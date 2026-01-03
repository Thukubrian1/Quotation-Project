package com.paymentservice.paymentservice.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO for STK Query Request
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StkQueryResponse {
    @JsonProperty("ResponseCode")
    private String responseCode;

    @JsonProperty("ResponseDescription")
    private String responseDescription;

    @JsonProperty("MerchantRequestID")
    private String merchantRequestID;

    @JsonProperty("CheckoutRequestID")
    private String checkoutRequestID;

    @JsonProperty("ResultCode")
    private String resultCode;

    @JsonProperty("ResultDesc")
    private String resultDesc;

    @JsonProperty("MpesaReceiptNumber")
    private String mpesaReceiptNumber;

    // Note: M-Pesa might return these fields for successful payments
    @JsonProperty("Amount")
    private String amount;

    @JsonProperty("Balance")
    private String balance;

    @JsonProperty("TransactionDate")
    private String transactionDate;

    @JsonProperty("PhoneNumber")
    private String phoneNumber;
}