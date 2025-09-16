package com.paymentservice.paymentservice.Controller;

import com.paymentservice.paymentservice.Entity.CardPaymentTransaction;
import com.paymentservice.paymentservice.Service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/transactions")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TransactionController {

        private final TransactionService transactionService;

        @GetMapping("/{transactionId}")
        public ResponseEntity<CardPaymentTransaction> getTransaction(@PathVariable String transactionId) {
            Optional<CardPaymentTransaction> transaction = transactionService.getTransaction(transactionId);

            if (transaction.isPresent()) {
                return ResponseEntity.ok(transaction.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        }

        @GetMapping("/merchant/{merchantId}")
        public ResponseEntity<List<CardPaymentTransaction>> getTransactionsByMerchant(@PathVariable String merchantId) {
            List<CardPaymentTransaction> transactions = transactionService.getTransactionsByMerchant(merchantId);
            return ResponseEntity.ok(transactions);
        }
}
