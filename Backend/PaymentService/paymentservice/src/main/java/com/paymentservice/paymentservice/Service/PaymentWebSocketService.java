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
     * Send payment update to a specific checkout request ID
     * Frontend subscribes to: /topic/payment/{checkoutRequestId}
     */
    public void sendPaymentUpdate(String checkoutRequestId, PaymentStatusMessage message) {
        try {
            String destination = "/topic/payment/" + checkoutRequestId;
            log.info("Sending WebSocket update to {}: status={}, message={}",
                    destination, message.getStatus(), message.getStatusMessage());

            messagingTemplate.convertAndSend(destination, message);

            log.debug("WebSocket message sent successfully to {}", destination);
        } catch (Exception e) {
            log.error("Failed to send WebSocket message for checkoutRequestId: {}",
                    checkoutRequestId, e);
        }
    }

    /**
     * Send payment initiation notification
     */
    public void sendPaymentInitiated(String checkoutRequestId, PaymentStatusMessage message) {
        try {
            String destination = "/topic/payment/" + checkoutRequestId;
            log.info("Sending payment initiated notification to {}", destination);
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.error("Failed to send payment initiated notification", e);
        }
    }

    /**
     * Send general payment notification to all subscribers
     */
    public void broadcastPaymentStatus(PaymentStatusMessage message) {
        try {
            log.info("Broadcasting payment status update: checkoutRequestId={}",
                    message.getCheckoutRequestId());
            messagingTemplate.convertAndSend("/topic/payments", message);
        } catch (Exception e) {
            log.error("Failed to broadcast payment status", e);
        }
    }
}