package com.shared.sharedlib.Enums;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
public enum PaymentStatus {

    PENDING("Payment is pending processing"),
    PROCESSING("Payment is currently being processed"),
    COMPLETED("Payment has been successfully completed"),
    FAILED("Payment failed to process"),
    CANCELLED("Payment was cancelled by user or system"),
    REFUNDED("Payment has been refunded"),
    PARTIALLY_REFUNDED("Payment has been partially refunded"),
    EXPIRED("Payment has expired"),
    DECLINED("Payment was declined by payment provider"),
    AUTHORIZED("Payment has been authorized but not captured"),
    CAPTURED("Payment has been captured after authorization"),
    VOIDED("Payment authorization has been voided");

    private final String description;

}
