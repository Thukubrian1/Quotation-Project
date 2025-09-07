package com.paymentservice.paymentservice.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mpesa")
@Data
public class MpesaProperties {
    private String consumerKey;
    private String consumerSecret;
    private String baseUrl;
    private String businessShortCode;
    private String passkey;
    private String callbackUrl;
}