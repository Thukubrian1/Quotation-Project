package com.paymentservice.paymentservice.Service;

import com.paymentservice.paymentservice.DTOs.PaymentStatusMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send payment status update to specific checkout request ID subscribers
     */
    public void sendPaymentUpdate(String checkoutRequestId, PaymentStatusMessage statusMessage) {
        try {
            String destination = "/topic/payment-status/" + checkoutRequestId;

            log.info("Sending WebSocket update to destination: {} for status: {}",
                    destination, statusMessage.getStatus());

            messagingTemplate.convertAndSend(destination, statusMessage);

            log.info("✅ WebSocket message sent successfully to {} subscribers", destination);

        } catch (Exception e) {
            log.error("❌ Failed to send WebSocket payment update for CheckoutRequestID: {}",
                    checkoutRequestId, e);
        }
    }

    /**
     * Send broadcast payment update to all payment subscribers
     */
    public void sendBroadcastPaymentUpdate(PaymentStatusMessage statusMessage) {
        try {
            String destination = "/topic/payment-updates";

            log.info("Sending broadcast WebSocket update for CheckoutRequestID: {}",
                    statusMessage.getCheckoutRequestId());

            messagingTemplate.convertAndSend(destination, statusMessage);

        } catch (Exception e) {
            log.error("Failed to send broadcast payment update", e);
        }
    }

    /**
     * Send test message (for debugging)
     */
    public void sendTestMessage(String message) {
        try {
            String destination = "/topic/test";
            messagingTemplate.convertAndSend(destination, message);
            log.info("Test message sent: {}", message);
        } catch (Exception e) {
            log.error("Failed to send test message", e);
        }
    }
}