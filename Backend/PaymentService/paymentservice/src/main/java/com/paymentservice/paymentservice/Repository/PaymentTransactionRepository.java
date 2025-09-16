package com.paymentservice.paymentservice.Repository;

import com.paymentservice.paymentservice.Entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByMerchantRequestId(String merchantRequestId);

    Optional<PaymentTransaction> findByCheckoutRequestId(String checkoutRequestId);

    // Additional useful queries
    List<PaymentTransaction> findByPhoneNumber(String phoneNumber);

    List<PaymentTransaction> findByStatus(String status);

    List<PaymentTransaction> findByAccountReference(String accountReference);
}