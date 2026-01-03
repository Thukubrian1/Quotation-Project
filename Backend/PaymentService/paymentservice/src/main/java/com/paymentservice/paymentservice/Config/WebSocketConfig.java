package com.paymentservice.paymentservice.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for topics - clients subscribe to /topic/*
        config.enableSimpleBroker("/topic");

        // Set application destination prefix for messages from clients
        config.setApplicationDestinationPrefixes("/app");

        log.info("✅ WebSocket message broker configured - Broker: /topic, App prefix: /app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint at /ws-payment
        // CRITICAL: This must match the frontend wsUrl: 'http://localhost:8081/ws-payment'
        registry.addEndpoint("/ws-payment")
                .setAllowedOriginPatterns("*") // Allow all origins for development
                .withSockJS(); // Enable SockJS fallback

        log.info("✅ STOMP endpoint registered at /ws-payment with SockJS support");
        log.info("📡 Frontend should connect to: http://localhost:8081/ws-payment");
        log.info("📨 Clients subscribe to: /topic/payment/{checkoutRequestId}");
    }
}