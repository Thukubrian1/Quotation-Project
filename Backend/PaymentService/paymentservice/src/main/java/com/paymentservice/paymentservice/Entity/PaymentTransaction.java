package com.paymentservice.paymentservice.Entity;

import com.shared.sharedlib.Enums.PaymentStatus;
import jakarta.persistence.Entity;
import jakarta.persistence. *;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "merchant_request_id")
    private String merchantRequestId;

    @Column(name = "checkout_request_id")
    private String checkoutRequestId;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "amount")
    private String amount;

    @Column(name = "account_reference")
    private String accountReference;

    @Column(name = "transaction_description")
    private String transactionDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status;

    @Column(name = "mpesa_receipt_number")
    private String mpesaReceiptNumber;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}