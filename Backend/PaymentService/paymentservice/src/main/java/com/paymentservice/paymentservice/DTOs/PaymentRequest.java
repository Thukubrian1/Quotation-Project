package com.paymentservice.paymentservice.DTOs;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "Phone number is required")
    // More flexible pattern to accept multiple formats: 0XXXXXXXXX, 254XXXXXXXXX, or XXXXXXXXX
    @Pattern(regexp = "^(254|0)?[17]\\d{8}$", message = "Phone number must be a valid Kenyan number (e.g., 0712345678, 254712345678, or 712345678)")
    private String phoneNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be at least 1.0")
    @DecimalMax(value = "70000.0", message = "Amount cannot exceed 70,000")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Account reference is required")
    @Size(max = 20, message = "Account reference cannot exceed 20 characters")
    private String accountReference;

    @Size(max = 100, message = "Transaction description cannot exceed 100 characters")
    private String transactionDescription;
}