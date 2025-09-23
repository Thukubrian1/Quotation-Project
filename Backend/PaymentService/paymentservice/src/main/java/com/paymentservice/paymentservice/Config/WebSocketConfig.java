package com.paymentservice.paymentservice.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for topics
        config.enableSimpleBroker("/topic");
        // Set application destination prefix
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint with SockJS fallback
        registry.addEndpoint("/ws-payment")
                .setAllowedOriginPatterns("*") // For development - be more specific in production
                .setAllowedOrigins("http://localhost:4200", "http://localhost:4200/") // Your Angular dev server
                .withSockJS()
                .setSessionCookieNeeded(false); // Disable cookies for WebSocket

        // Also add without SockJS for native WebSocket support
        registry.addEndpoint("/ws-payment")
                .setAllowedOriginPatterns("*")
                .setAllowedOrigins("http://localhost:4200", "http://localhost:4200/");
    }
}