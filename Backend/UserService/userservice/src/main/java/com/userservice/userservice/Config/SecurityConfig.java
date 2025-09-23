package com.userservice.userservice.Config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Profile("!dev")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // IMPORTANT: Enable CORS first - this must come before other configurations
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**") // Disable CSRF for H2 console
                        .disable() // Disable CSRF for REST API
                )
                .headers(headers -> headers
                        .addHeaderWriter(new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN))
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints - no authentication required
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/users/login").permitAll()  // Allow login without auth
                        .requestMatchers("/users/adduser").permitAll() // Allow user registration without auth
                        .requestMatchers("/users/test").permitAll() // Allow test endpoint without auth
                        .requestMatchers("/health", "/actuator/health").permitAll()
                        .requestMatchers("/users/ping").permitAll() // Allow ping without auth
                        .requestMatchers("/users/check-email").permitAll() // Allow email check without auth
                        // Allow OPTIONS requests for CORS preflight
                        .requestMatchers("OPTIONS", "/**").permitAll()
                        // Protected endpoints - require authentication
                        .requestMatchers("/users/getuser").authenticated()
                        .requestMatchers("/users/**").authenticated() // Other user endpoints require auth
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.setHeader("Access-Control-Allow-Origin", "*");
                            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                            response.setHeader("Access-Control-Allow-Headers", "*");
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + authException.getMessage() + "\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(401); // Return 401 instead of 403
                            response.setContentType("application/json");
                            response.setHeader("Access-Control-Allow-Origin", "*");
                            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                            response.setHeader("Access-Control-Allow-Headers", "*");
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Access denied\"}");
                        })
                )
                .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Profile("dev")
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http
                // IMPORTANT: Enable CORS first - this must come before other configurations
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                .csrf(csrf -> csrf.disable()) // Disable CSRF completely in dev
                .headers(headers -> headers.disable()) // Disable all security headers in dev for H2 console
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints - no authentication required
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/users/login").permitAll()  // Allow login without auth
                        .requestMatchers("/users/adduser").permitAll() // Allow user registration without auth
                        .requestMatchers("/users/test").permitAll() // Allow test endpoint without auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/users/ping").permitAll() // Allow ping without auth
                        .requestMatchers("/users/check-email").permitAll() // Allow email check without auth
                        .requestMatchers("/users/health").permitAll() // Allow health check without auth
                        // Allow OPTIONS requests for CORS preflight
                        .requestMatchers("OPTIONS", "/**").permitAll()
                        // In dev mode, allow some endpoints without auth for testing
                        .requestMatchers("/users/getuser").authenticated()
                        .requestMatchers("/users/**").authenticated()
                        .anyRequest().permitAll() // Allow all other requests in dev
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.setHeader("Access-Control-Allow-Origin", "*");
                            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                            response.setHeader("Access-Control-Allow-Headers", "*");
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + authException.getMessage() + "\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(401); // Return 401 instead of 403
                            response.setContentType("application/json");
                            response.setHeader("Access-Control-Allow-Origin", "*");
                            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                            response.setHeader("Access-Control-Allow-Headers", "*");
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Access denied\"}");
                        })
                )
                .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}