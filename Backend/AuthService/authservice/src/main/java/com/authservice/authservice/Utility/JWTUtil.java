package com.authservice.authservice.Utility;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JWTUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:3600000}")
    private long expiration;

    private Key getSigningKey() {
        // Ensure the secret is at least 256 bits (32 characters) for HS256
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters long");
        }
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username) {
        try {
            return Jwts.builder()
                    .setSubject(username)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + expiration))
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            log.error("Error generating JWT token for user: {}", username, e);
            throw new RuntimeException("Could not generate JWT token", e);
        }
    }

    public String extractUsername(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            log.debug("Token expired for extraction: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error extracting username from token", e);
            throw new RuntimeException("Invalid token", e);
        }
    }

    public Date extractExpiration(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration();
        } catch (ExpiredJwtException e) {
            log.debug("Token expired for expiration extraction: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error extracting expiration from token", e);
            throw new RuntimeException("Invalid token", e);
        }
    }

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            log.debug("Token expiration check failed: {}", e.getMessage());
            return true;
        }
    }

    private Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.debug("Token expired during claims extraction");
            throw e;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("JWT security exception - signature mismatch or malformed token");
            throw new RuntimeException("Invalid token signature", e);
        } catch (Exception e) {
            log.error("Error parsing JWT token", e);
            throw new RuntimeException("Invalid token", e);
        }
    }
}