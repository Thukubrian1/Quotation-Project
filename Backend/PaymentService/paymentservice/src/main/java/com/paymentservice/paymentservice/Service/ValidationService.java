package com.paymentservice.paymentservice.Service;

import com.paymentservice.paymentservice.DTOs.CardPaymentRequestDTO;
import com.paymentservice.paymentservice.DTOs.CardPaymentResponseDTO;
import com.paymentservice.paymentservice.Entity.CardPaymentTransaction;
import java.util.List;
import java.util.Optional;

public interface ValidationService {

    List<String> validateCardPaymentRequest(CardPaymentRequestDTO request);

    Optional<CardPaymentTransaction> getTransaction(String transactionId);

    List<CardPaymentTransaction> getTransactionsByMerchant(String merchantId);

    CardPaymentResponseDTO refundTransaction(String transactionId, String reason);
}
