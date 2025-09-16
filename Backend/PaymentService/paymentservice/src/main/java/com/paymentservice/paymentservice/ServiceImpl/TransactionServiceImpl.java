package com.paymentservice.paymentservice.ServiceImpl;

import com.paymentservice.paymentservice.DTOs.CardPaymentRequestDTO;
import com.paymentservice.paymentservice.DTOs.CardPaymentResponseDTO;
import com.paymentservice.paymentservice.Entity.CardPaymentTransaction;
import com.paymentservice.paymentservice.Repository.PaymentTransactionRepository;
import com.paymentservice.paymentservice.Repository.TransactionRepository;
import com.paymentservice.paymentservice.Service.CardProcessingService;
import com.paymentservice.paymentservice.Service.TokenizationService;
import com.paymentservice.paymentservice.Service.TransactionService;
import com.paymentservice.paymentservice.Service.ValidationService;
import com.shared.sharedlib.Enums.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final ValidationService validationService;
    private final TokenizationService tokenizationService;
    private final CardProcessingService cardProcessingService;

    @Override
    @Transactional
    public CardPaymentResponseDTO processCardPayment(CardPaymentRequestDTO request) {
        // 1. Validate request
        List<String> validationErrors = validationService.validateCardPaymentRequest(request);
        if (!validationErrors.isEmpty()) {
            return createErrorResponse("VALIDATION_FAILED", String.join(", ", validationErrors));
        }

        // 2. Create transaction entity
        CardPaymentTransaction transaction = CardPaymentTransaction.builder()
                .id(UUID.randomUUID().toString())
                .merchantId(request.getMerchantId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .status(TransactionStatus.PROCESSING)
                .createdAt(LocalDateTime.now())
                .build();

        // 3. Tokenize card
        String cardType = getCardType(request.getCardNumber());
        String tokenId = tokenizationService.tokenizeCard(
                request.getCardNumber(),
                request.getCvv(),
                request.getExpiryMonth(),
                request.getExpiryYear(),
                request.getCardholderName(),
                cardType
        );

        transaction.setCardTokenId(tokenId);
        transaction.setCardLastFourDigits(request.getCardNumber().substring(request.getCardNumber().length() - 4));
        transaction.setCardType(cardType);

        // Save transaction
        transaction = transactionRepository.save(transaction);

        // 4. Process payment with bank
        CardProcessingServiceImpl.BankResponse bankResponse = (CardProcessingServiceImpl.BankResponse)
                cardProcessingService.processPayment(
                        request.getCardNumber(),
                        request.getCvv(),
                        request.getExpiryMonth(),
                        request.getExpiryYear(),
                        request.getCardholderName(),
                        request.getAmount(),
                        request.getCurrency(),
                        transaction.getId()
                );

        // 5. Update transaction based on bank response
        transaction.setBankTransactionId(bankResponse.getTransactionId());
        transaction.setAuthorizationCode(bankResponse.getAuthorizationCode());
        transaction.setBankResponseCode(bankResponse.getResponseCode());
        transaction.setBankResponseMessage(bankResponse.getResponseMessage());
        transaction.setProcessedAt(LocalDateTime.now());

        if (bankResponse.isSuccess()) {
            transaction.setStatus(TransactionStatus.COMPLETED);
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
        }

        transaction = transactionRepository.save(transaction);

        // 6. Create response
        return CardPaymentResponseDTO.builder()
                .transactionId(transaction.getId())
                .status(transaction.getStatus().toString())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .merchantId(transaction.getMerchantId())
                .authorizationCode(transaction.getAuthorizationCode())
                .bankResponseCode(transaction.getBankResponseCode())
                .bankResponseMessage(transaction.getBankResponseMessage())
                .processedAt(transaction.getProcessedAt())
                .cardLastFourDigits(transaction.getCardLastFourDigits())
                .cardType(transaction.getCardType())
                .build();
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
    public List<CardPaymentTransaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @Override
    @Transactional
    public CardPaymentResponseDTO refundTransaction(String transactionId, String reason) {
        return validationService.refundTransaction(transactionId, reason);
    }

    @Override
    @Transactional
    public CardPaymentResponseDTO voidTransaction(String transactionId) {
        Optional<CardPaymentTransaction> optionalTransaction = transactionRepository.findById(transactionId);

        if (optionalTransaction.isEmpty()) {
            return createErrorResponse("TRANSACTION_NOT_FOUND", "Transaction not found");
        }

        CardPaymentTransaction transaction = optionalTransaction.get();

        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            return createErrorResponse("INVALID_STATUS", "Can only void completed transactions");
        }

        transaction.setStatus(TransactionStatus.VOIDED);
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        return CardPaymentResponseDTO.builder()
                .transactionId(transaction.getId())
                .status("VOIDED")
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .processedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public List<CardPaymentTransaction> getTransactionsByStatus(String status) {
        TransactionStatus transactionStatus = TransactionStatus.valueOf(status.toUpperCase());
        return transactionRepository.findByStatusOrderByCreatedAtDesc(transactionStatus);
    }

    private String getCardType(String cardNumber) {
        if (cardNumber.startsWith("4")) return "VISA";
        if (cardNumber.startsWith("5") || cardNumber.startsWith("2")) return "MASTERCARD";
        if (cardNumber.startsWith("3")) return "AMEX";
        return "UNKNOWN";
    }

    private CardPaymentResponseDTO createErrorResponse(String status, String message) {
        return CardPaymentResponseDTO.builder()
                .status(status)
                .bankResponseMessage(message)
                .build();
    }
}