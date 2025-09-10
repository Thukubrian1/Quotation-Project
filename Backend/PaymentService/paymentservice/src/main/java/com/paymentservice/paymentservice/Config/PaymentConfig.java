package com.paymentservice.paymentservice.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class PaymentConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        // Default RestTemplate for calling external services (like auth service)
        return new RestTemplate();
    }

    @Bean("mpesaRestTemplate")
    public RestTemplate mpesaRestTemplate() {
        // Separate RestTemplate for M-Pesa API calls
        return new RestTemplate();
    }
}