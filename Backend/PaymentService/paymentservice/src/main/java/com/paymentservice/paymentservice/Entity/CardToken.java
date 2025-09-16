package com.paymentservice.paymentservice.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "card_tokens")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CardToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "token_id")
    private String tokenId;

    @Column(name = "encrypted_card_data", columnDefinition = "TEXT")
    private String encryptedCardData;

    @Column(name = "last_four_digits")
    private String lastFourDigits;

    @Column(name = "card_type")
    private String cardType;

    @Column(name = "expiry_month")
    private String expiryMonth;

    @Column(name = "expiry_year")
    private String expiryYear;

    @Column(name = "card_holder_name")
    private String cardholderName;

    @Column(name = "createdat")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}