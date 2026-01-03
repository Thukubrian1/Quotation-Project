package com.paymentservice.paymentservice.Config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS with custom configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Disable CSRF for REST APIs
                .csrf(csrf -> csrf.disable())

                // Configure session management
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure authorization
                .authorizeHttpRequests(authz -> authz
                        // Allow all OPTIONS requests (for CORS preflight)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Allow public endpoints
                        .requestMatchers("/api/v1/payments/public-health").permitAll()
                        .requestMatchers("/api/v1/payments/mpesa/callback").permitAll()

                        // Allow Swagger/OpenAPI endpoints (if you're using them)
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Allow actuator endpoints (optional, for monitoring)
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // All other API endpoints require authentication
                        .requestMatchers("/api/**").authenticated()

                        // Allow all other requests (you might want to be more restrictive)
                        .anyRequest().permitAll())

                // Add custom authentication filter
                .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Configure exception handling
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(401);
                            response.getWriter().write("""
                                    {
                                        "status": "ERROR",
                                        "message": "Authentication required",
                                        "debugMessage": "Please provide a valid Bearer token"
                                    }
                                    """);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json");
                            response.setStatus(403);
                            response.getWriter().write("""
                                    {
                                        "status": "ERROR",
                                        "message": "Access denied",
                                        "debugMessage": "You don't have permission to access this resource"
                                    }
                                    """);
                        }));

        return http.build();
    }
}