package com.paymentservice.paymentservice.Repository;

import com.paymentservice.paymentservice.Entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, String> {

    Optional<Merchant> findByMerchantId(String merchantId);

    Optional<Merchant> findByApiKey(String apiKey);

    List<Merchant> findByActiveTrue();

    @Query("SELECT m FROM Merchant m WHERE m.businessName LIKE %:businessName%")
    List<Merchant> findByBusinessNameContaining(@Param("businessName") String businessName);

    boolean existsByMerchantId(String merchantId);

    boolean existsByApiKey(String apiKey);

}
