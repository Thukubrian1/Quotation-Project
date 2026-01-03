
package com.paymentservice.paymentservice.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO for STK Query Request
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StkQueryRequest {
    @JsonProperty("BusinessShortCode")
    private String businessShortCode;

    @JsonProperty("Password")
    private String password;

    @JsonProperty("Timestamp")
    private String timestamp;

    @JsonProperty("CheckoutRequestID")
    private String checkoutRequestID;
}