package com.paymentservice.paymentservice.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mpesa.daraja")
@Data
@Configuration
public class MpesaProperties {
    private String consumerKey;
    private String consumerSecret;
    private String stkPushRequestUrl;
    private String businessShortCode; // Changed from stkPushShortCode
    private String passKey; // Changed from stkPassKey
    private String callbackUrl; // Changed from stkPushCallbackUrl
    private String oauthEndpoint;

}

