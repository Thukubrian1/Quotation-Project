package com.paymentservice.paymentservice.Service;

import com.paymentservice.paymentservice.DTOs.CardPaymentRequestDTO;
import com.paymentservice.paymentservice.DTOs.CardPaymentResponseDTO;
import com.paymentservice.paymentservice.Entity.CardPaymentTransaction;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

public interface TransactionService {

    CardPaymentResponseDTO processCardPayment(CardPaymentRequestDTO request);

    CardPaymentResponseDTO refundTransaction(String transactionId, String reason);

    Optional<CardPaymentTransaction> getTransaction(String transactionId);

    List<CardPaymentTransaction> getTransactionsByMerchant(String merchantId);

    List<CardPaymentTransaction> getAllTransactions();

    CardPaymentResponseDTO voidTransaction(String transactionId);

    List<CardPaymentTransaction> getTransactionsByStatus(String status);
}