package com.paymentservice.paymentservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardPaymentResponseDTO {

    private String transactionId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String merchantId;
    private String authorizationCode;
    private String bankResponseCode;
    private String bankResponseMessage;
    private LocalDateTime processedAt;
    private String cardLastFourDigits;
    private String cardType;
}
