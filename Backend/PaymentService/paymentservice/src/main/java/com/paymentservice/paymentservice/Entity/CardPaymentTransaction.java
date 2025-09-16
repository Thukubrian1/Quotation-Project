package com.paymentservice.paymentservice.Entity;

import com.shared.sharedlib.Enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_payment_transactions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CardPaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "merchant_id")
    private String merchantId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(name = "card_token_id")
    private String cardTokenId;

    @Column(name = "card_last_four_digits")
    private String cardLastFourDigits;

    @Column(name = "card_type")
    private String cardType;

    @Column(name = "checkout_request_id")
    private String checkoutRequestId;

    @Column(name = "authorization_code")
    private String authorizationCode;

    @Column(name = "bank_transaction_id")
    private String bankTransactionId;

    @Column(name = "bank_response_code")
    private String bankResponseCode;

    @Column(name = "bank_response_message")
    private String bankResponseMessage;

    @Column(name = "description")
    private String description;

    @Column(name = "createdat")
    private LocalDateTime createdAt;

    @Column(name = "updatedat")
    private LocalDateTime updatedAt;

    @Column(name = "processedat")
    private LocalDateTime processedAt;
}
