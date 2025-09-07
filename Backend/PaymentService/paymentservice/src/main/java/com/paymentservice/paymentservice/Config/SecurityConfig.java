package com.paymentservice.paymentservice.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**")
                        .ignoringRequestMatchers("/api/v1/payments/mpesa/callback")
                )
                .headers(headers -> headers
                        .frameOptions().sameOrigin() // Allow H2 console frames
                )
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/h2-console/**").permitAll() // Allow H2 console access
                        .requestMatchers("/api/v1/payments/mpesa/callback").permitAll() // M-Pesa callback
                        .requestMatchers("/actuator/**").permitAll() // Actuator endpoints
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {}) // Use default JWT configuration
                );

        return http.build();
    }

    // Development profile specific configuration
    @Bean
    @Profile("dev")
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions().disable())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/v1/payments/mpesa/callback").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().permitAll() // Allow all requests in dev mode
                );

        return http.build();
    }
}