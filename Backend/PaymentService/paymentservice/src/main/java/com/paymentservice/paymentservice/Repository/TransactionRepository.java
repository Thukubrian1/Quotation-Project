package com.paymentservice.paymentservice.Repository;

import com.paymentservice.paymentservice.Entity.CardPaymentTransaction;
import com.paymentservice.paymentservice.Entity.PaymentTransaction;
import com.shared.sharedlib.Enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<CardPaymentTransaction, String> {

    List<CardPaymentTransaction> findByMerchantIdOrderByCreatedAtDesc(String merchantId);

    List<CardPaymentTransaction> findByStatusOrderByCreatedAtDesc(TransactionStatus status);

    Optional<CardPaymentTransaction> findByBankTransactionId(String bankTransactionId);

    @Query("SELECT t FROM CardPaymentTransaction t WHERE t.merchantId = :merchantId AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<CardPaymentTransaction> findTransactionsByMerchantAndDateRange(
            @Param("merchantId") String merchantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT SUM(t.amount) FROM CardPaymentTransaction t WHERE t.merchantId = :merchantId AND t.status = :status")
    BigDecimal getTotalAmountByMerchantAndStatus(
            @Param("merchantId") String merchantId,
            @Param("status") TransactionStatus status
    );

    @Query("SELECT COUNT(t) FROM CardPaymentTransaction t WHERE t.status = :status AND t.createdAt >= :startDate")
    Long countTransactionsByStatusSince(
            @Param("status") TransactionStatus status,
            @Param("startDate") LocalDateTime startDate
    );

    Optional<PaymentTransaction> findByCheckoutRequestId(String checkoutRequestId);
}
