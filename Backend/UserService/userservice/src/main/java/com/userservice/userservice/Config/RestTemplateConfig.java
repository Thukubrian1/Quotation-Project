package com.userservice.userservice.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@Configuration
@Slf4j
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(clientHttpRequestFactory());

        // Add logging interceptor for debugging
        restTemplate.setInterceptors(List.of(loggingInterceptor()));

        return restTemplate;
    }

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // Set connection timeout (10 seconds)
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());

        // Set read timeout (30 seconds)
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());

        return factory;
    }

    @Bean
    public ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            log.debug("REST Request: {} {}", request.getMethod(), request.getURI());
            var response = execution.execute(request, body);
            log.debug("REST Response: {}", response.getStatusCode());
            return response;
        };
    }
}