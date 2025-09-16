package com.paymentservice.paymentservice.ServiceImpl;

import com.paymentservice.paymentservice.Entity.CardToken;
import com.paymentservice.paymentservice.Repository.CardTokenRepository;
import com.paymentservice.paymentservice.Service.TokenizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenizationServiceImpl implements TokenizationService {

    private final CardTokenRepository cardTokenRepository;

    // In production, load this from secure configuration
    private final String ENCRYPTION_KEY = "MySecretKey12345"; // 16 bytes for AES-128

    public String tokenizeCard(String cardNumber, String cvv, String expiryMonth,
                               String expiryYear, String cardholderName, String cardType) {
        try {
            // Generate unique token
            String tokenId = generateUniqueToken();

            // Create card data string to encrypt
            String cardData = cardNumber + "|" + cvv + "|" + expiryMonth + "|" + expiryYear + "|" + cardholderName;

            // Encrypt card data
            String encryptedCardData = encrypt(cardData);

            // Create token entity
            CardToken tokenEntity = new CardToken();
            tokenEntity.setTokenId(tokenId);
            tokenEntity.setEncryptedCardData(encryptedCardData);
            tokenEntity.setLastFourDigits(cardNumber.substring(cardNumber.length() - 4));
            tokenEntity.setCardType(cardType);
            tokenEntity.setExpiryMonth(expiryMonth);
            tokenEntity.setExpiryYear(expiryYear);
            tokenEntity.setCardholderName(cardholderName);
            tokenEntity.setExpiresAt(LocalDateTime.now().plusYears(1)); // Token expires in 1 year

            cardTokenRepository.save(tokenEntity);

            return tokenId;
        } catch (Exception e) {
            throw new RuntimeException("Failed to tokenize card", e);
        }
    }

    public Optional<CardToken> getTokenData(String tokenId) {
        return cardTokenRepository.findByTokenId(tokenId);
    }

    public String decryptCardData(String encryptedData) {
        try {
            return decrypt(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt card data", e);
        }
    }

    private String generateUniqueToken() {
        return "tok_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String encrypt(String data) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
    }

    private String decrypt(String encryptedData) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedData)));
    }
}
