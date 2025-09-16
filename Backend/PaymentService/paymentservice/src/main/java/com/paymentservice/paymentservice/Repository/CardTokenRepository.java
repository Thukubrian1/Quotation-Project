package com.paymentservice.paymentservice.Repository;

import com.paymentservice.paymentservice.Entity.CardToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CardTokenRepository extends JpaRepository<CardToken, String> {

    Optional<CardToken> findByTokenId(String tokenId);

    void deleteByTokenId(String tokenId);
}
