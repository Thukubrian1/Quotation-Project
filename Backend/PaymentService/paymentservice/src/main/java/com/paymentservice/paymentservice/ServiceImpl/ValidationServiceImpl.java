package com.paymentservice.paymentservice.ServiceImpl;

import com.paymentservice.paymentservice.DTOs.CardPaymentRequestDTO;
import com.paymentservice.paymentservice.DTOs.CardPaymentResponseDTO;
import com.paymentservice.paymentservice.Entity.CardPaymentTransaction;
import com.paymentservice.paymentservice.Repository.TransactionRepository;
import com.paymentservice.paymentservice.Service.CardProcessingService;
import com.paymentservice.paymentservice.Service.ValidationService;
import com.shared.sharedlib.Enums.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ValidationServiceImpl implements ValidationService {

    private final TransactionRepository transactionRepository;
    private final CardProcessingService cardProcessingService;

    @Override
    public List<String> validateCardPaymentRequest(CardPaymentRequestDTO request) {
        List<String> errors = new ArrayList<>();

        // Validate card number using Luhn algorithm
        if (!isValidCardNumber(request.getCardNumber())) {
            errors.add("Invalid card number");
        }

        // Validate expiry date
        if (!isValidExpiryDate(request.getExpiryMonth(), request.getExpiryYear())) {
            errors.add("Card has expired or invalid expiry date");
        }

        // Validate CVV
        if (!isValidCVV(request.getCvv())) {
            errors.add("Invalid CVV format");
        }

        // Validate amount
        if (request.getAmount() == null || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            errors.add("Amount must be greater than zero");
        }

        // Validate currency
        if (!isValidCurrency(request.getCurrency())) {
            errors.add("Invalid currency code");
        }

        return errors;
    }

    @Override
    public Optional<CardPaymentTransaction> getTransaction(String transactionId) {
        return transactionRepository.findById(transactionId);
    }

    @Override
    public List<CardPaymentTransaction> getTransactionsByMerchant(String merchantId) {
        return transactionRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    @Override
    @Transactional
    public CardPaymentResponseDTO refundTransaction(String transactionId, String reason) {
        Optional<CardPaymentTransaction> optionalTransaction = transactionRepository.findById(transactionId);

        if (optionalTransaction.isEmpty()) {
            return createErrorResponse("TRANSACTION_NOT_FOUND", "Transaction not found");
        }

        CardPaymentTransaction originalTransaction = optionalTransaction.get();

        if (originalTransaction.getStatus() != TransactionStatus.COMPLETED) {
            return createErrorResponse("INVALID_STATUS", "Can only refund completed transactions");
        }

        // Process refund with bank
        CardProcessingServiceImpl.BankResponse refundResponse = (CardProcessingServiceImpl.BankResponse)
                cardProcessingService.refundPayment(
                        originalTransaction.getBankTransactionId(),
                        originalTransaction.getAmount()
                );

        if (refundResponse.isSuccess()) {
            originalTransaction.setStatus(TransactionStatus.REFUNDED);
            originalTransaction.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(originalTransaction);

            return CardPaymentResponseDTO.builder()
                    .transactionId(originalTransaction.getId())
                    .status("REFUNDED")
                    .amount(originalTransaction.getAmount())
                    .currency(originalTransaction.getCurrency())
                    .processedAt(LocalDateTime.now())
                    .build();
        } else {
            return createErrorResponse("REFUND_FAILED", refundResponse.getResponseMessage());
        }
    }

    // Helper methods
    private boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 13 || cardNumber.length() > 19) {
            return false;
        }
        return luhnCheck(cardNumber);
    }

    private boolean luhnCheck(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    private boolean isValidExpiryDate(String month, String year) {
        try {
            int expMonth = Integer.parseInt(month);
            int expYear = Integer.parseInt(year);

            if (expMonth < 1 || expMonth > 12) {
                return false;
            }

            LocalDate now = LocalDate.now();
            LocalDate expiry = LocalDate.of(expYear, expMonth, 1).plusMonths(1).minusDays(1);

            return expiry.isAfter(now);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidCVV(String cvv) {
        return cvv != null && cvv.matches("^[0-9]{3,4}$");
    }

    private boolean isValidCurrency(String currency) {
        // Add more currencies as needed
        List<String> validCurrencies = List.of("USD", "EUR", "GBP", "KES", "UGX", "TZS");
        return validCurrencies.contains(currency.toUpperCase());
    }

    private CardPaymentResponseDTO createErrorResponse(String status, String message) {
        return CardPaymentResponseDTO.builder()
                .status(status)
                .bankResponseMessage(message)
                .build();
    }
}
