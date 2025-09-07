package com.paymentservice.paymentservice.Repository;

import com.paymentservice.paymentservice.Entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Integer> {

    Optional<PaymentTransaction> findByMerchantRequestId(String merchantRequestId);
    Optional<PaymentTransaction> findByCheckoutRequestId(String checkoutRequestId);
}
