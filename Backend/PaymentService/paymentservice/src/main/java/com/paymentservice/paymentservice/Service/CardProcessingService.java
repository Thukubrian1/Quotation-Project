package com.paymentservice.paymentservice.Service;

import com.paymentservice.paymentservice.ServiceImpl.CardProcessingServiceImpl;
import java.math.BigDecimal;

public interface CardProcessingService {

    CardProcessingServiceImpl.BankResponse processPayment(String cardNumber, String cvv, String expiryMonth,
                                                          String expiryYear, String cardholderName,
                                                          BigDecimal amount, String currency, String transactionId);

    CardProcessingServiceImpl.BankResponse refundPayment(String originalTransactionId, BigDecimal amount);
}
