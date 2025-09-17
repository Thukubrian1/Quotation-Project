package com.userservice.userservice.Config;

import com.userservice.userservice.Service.AuthServiceClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final AuthServiceClient authServiceClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String bearerToken = getBearerTokenFromRequest(request);
            log.debug("Processing request to: {}", request.getRequestURI());
            log.debug("Bearer token present: {}", bearerToken != null);

            if (StringUtils.hasText(bearerToken)) {
                // Validate token with auth service
                boolean isValidToken = validateTokenWithAuthService(bearerToken);

                if (isValidToken) {
                    // Create authentication token with USER role
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_USER")
                    );

                    // Use "authenticated-user" as placeholder since we don't extract username from token
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken("authenticated-user", null, authorities);

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("Successfully authenticated request with bearer token");
                } else {
                    log.warn("Invalid bearer token for request to: {}", request.getRequestURI());
                    // Clear any existing authentication
                    SecurityContextHolder.clearContext();
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context for request to: {}",
                    request.getRequestURI(), ex);
            // Clear authentication on error
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String getBearerTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean validateTokenWithAuthService(String token) {
        try {
            log.debug("Validating token with auth service: {}", token.substring(0, Math.min(20, token.length())) + "...");
            boolean isValid = authServiceClient.validateToken(token);
            log.debug("Token validation result: {}", isValid);
            return isValid;
        } catch (Exception e) {
            log.error("Error validating token with auth service", e);
            return false;
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        log.debug("Checking shouldNotFilter for path: {}", path);

        // Skip token validation for these endpoints
        boolean shouldSkip =
                path.equals("/health") ||
                path.equals("/actuator/health") ||
                path.startsWith("/h2-console") ||  // Allow H2 console access
                (path.startsWith("/h2-console") && request.getHeader("Profile") != null &&
                        request.getHeader("Profile").equals("dev"));

        log.debug("Should skip token validation: {}", shouldSkip);
        return shouldSkip;
    }
}