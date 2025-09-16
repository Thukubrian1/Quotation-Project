package com.paymentservice.paymentservice.Service;

import com.paymentservice.paymentservice.Entity.CardToken;
import org.springframework.stereotype.Service;
import java.util.Optional;

public interface TokenizationService {

    String tokenizeCard(String cardNumber, String cvv, String expiryMonth,
                 String expiryYear, String cardholderName, String cardType);

    Optional<CardToken> getTokenData(String tokenId);

    String decryptCardData(String encryptedData);
}
