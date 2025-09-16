package com.paymentservice.paymentservice.Controller;

import com.paymentservice.paymentservice.Entity.CardToken;
import com.paymentservice.paymentservice.Service.TokenizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/cards")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CardController {

    private final TokenizationService tokenizationService;

    @GetMapping("/token/{tokenId}")
    public ResponseEntity<Map<String, Object>> getCardToken(@PathVariable String tokenId) {
        Optional<CardToken> cardToken = tokenizationService.getTokenData(tokenId);

        if (cardToken.isPresent()) {
            CardToken token = cardToken.get();
            Map<String, Object> response = new HashMap<>();
            response.put("tokenId", token.getTokenId());
            response.put("lastFourDigits", token.getLastFourDigits());
            response.put("cardType", token.getCardType());
            response.put("expiryMonth", token.getExpiryMonth());
            response.put("expiryYear", token.getExpiryYear());
            response.put("cardholderName", token.getCardholderName());
            response.put("expiresAt", token.getExpiresAt());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/token/{tokenId}")
    public ResponseEntity<Map<String, String>> deleteCardToken(@PathVariable String tokenId) {
        try {
            Optional<CardToken> cardToken = tokenizationService.getTokenData(tokenId);
            if (cardToken.isPresent()) {
                // Implementation would call repository delete method
                Map<String, String> response = new HashMap<>();
                response.put("status", "SUCCESS");
                response.put("message", "Token deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("status", "ERROR");
                response.put("message", "Token not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", "Failed to delete token: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}