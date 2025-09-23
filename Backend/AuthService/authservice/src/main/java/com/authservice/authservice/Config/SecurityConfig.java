package com.authservice.authservice.Config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS with custom configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Disable CSRF for REST APIs
                .csrf(csrf -> csrf.disable())

                // Configure session management for stateless JWT
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()  // Allow all auth endpoints
                        .requestMatchers("/actuator/health").permitAll()  // Health check
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}