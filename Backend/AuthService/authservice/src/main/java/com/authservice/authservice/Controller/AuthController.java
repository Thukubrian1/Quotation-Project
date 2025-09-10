package com.authservice.authservice.Controller;

import com.authservice.authservice.Service.AuthService;
import com.authservice.authservice.Utility.JWTUtil;
import com.shared.sharedlib.Dtos.GenericResponse;
import com.shared.sharedlib.Enums.ResponseStatusEnum;
import com.shared.sharedlib.Exceptions.BusinessException;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JWTUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<GenericResponse<Map<String, String>>> login(
            @RequestParam String username,
            @RequestParam String password) {

        try {
            // Validate input parameters
            if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
                GenericResponse<Map<String, String>> response = GenericResponse.<Map<String, String>>builder()
                        .status(ResponseStatusEnum.BAD_REQUEST)
                        .message("Username and password are required")
                        .build();
                return ResponseEntity.status(ResponseStatusEnum.BAD_REQUEST.getHttpStatus()).body(response);
            }

            // Attempt login
            String token = authService.login(username.trim(), password.trim());

            // Prepare response data
            Map<String, String> tokenData = new HashMap<>();
            tokenData.put("access_token", token);
            tokenData.put("token_type", "Bearer");
            tokenData.put("username", username.trim());

            GenericResponse<Map<String, String>> response = GenericResponse.<Map<String, String>>builder()
                    .status(ResponseStatusEnum.SUCCESS)
                    .message("Login successful")
                    .data(tokenData)
                    .build();

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            log.warn("Business exception during login for username: {}", username, e);

            GenericResponse<Map<String, String>> response = GenericResponse.<Map<String, String>>builder()
                    .status(e.getStatus())
                    .message(e.getMessage())
                    .debugMessage(e.getDebugMessage())
                    .build();

            return ResponseEntity.status(e.getStatus().getHttpStatus()).body(response);

        } catch (Exception e) {
            log.error("Unexpected error during login for username: {}", username, e);

            GenericResponse<Map<String, String>> response = GenericResponse.<Map<String, String>>builder()
                    .status(ResponseStatusEnum.ERROR)
                    .message("An unexpected error occurred")
                    .debugMessage(e.getMessage())
                    .build();

            return ResponseEntity.status(ResponseStatusEnum.ERROR.getHttpStatus()).body(response);
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<GenericResponse<Map<String, Object>>> validateToken(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                GenericResponse<Map<String, Object>> response = GenericResponse.<Map<String, Object>>builder()
                        .status(ResponseStatusEnum.BAD_REQUEST)
                        .message("Missing or invalid Authorization header. Expected format: Bearer <token>")
                        .data(createValidationResponse(false, null, "Missing authorization header"))
                        .build();
                return ResponseEntity.status(ResponseStatusEnum.BAD_REQUEST.getHttpStatus()).body(response);
            }

            String token = authHeader.substring(7);

            if (!StringUtils.hasText(token)) {
                GenericResponse<Map<String, Object>> response = GenericResponse.<Map<String, Object>>builder()
                        .status(ResponseStatusEnum.BAD_REQUEST)
                        .message("Token is required")
                        .data(createValidationResponse(false, null, "Empty token"))
                        .build();
                return ResponseEntity.status(ResponseStatusEnum.BAD_REQUEST.getHttpStatus()).body(response);
            }

            // Validate token and extract username
            if (!jwtUtil.isTokenValid(token)) {
                GenericResponse<Map<String, Object>> response = GenericResponse.<Map<String, Object>>builder()
                        .status(ResponseStatusEnum.UNAUTHORIZED)
                        .message("Token has expired or is invalid")
                        .data(createValidationResponse(false, null, "Token expired or invalid"))
                        .build();
                return ResponseEntity.status(ResponseStatusEnum.UNAUTHORIZED.getHttpStatus()).body(response);
            }

            String username = jwtUtil.extractUsername(token);

            if (!StringUtils.hasText(username)) {
                GenericResponse<Map<String, Object>> response = GenericResponse.<Map<String, Object>>builder()
                        .status(ResponseStatusEnum.UNAUTHORIZED)
                        .message("Invalid token - no username found")
                        .data(createValidationResponse(false, null, "No username in token"))
                        .build();
                return ResponseEntity.status(ResponseStatusEnum.UNAUTHORIZED.getHttpStatus()).body(response);
            }

            GenericResponse<Map<String, Object>> response = GenericResponse.<Map<String, Object>>builder()
                    .status(ResponseStatusEnum.SUCCESS)
                    .message("Token is valid")
                    .data(createValidationResponse(true, username, null))
                    .build();

            return ResponseEntity.ok(response);

        }
        catch (ExpiredJwtException e) {
            log.warn("Token expired during validation");
            GenericResponse<Map<String, Object>> response = GenericResponse.<Map<String, Object>>builder()
                    .status(ResponseStatusEnum.UNAUTHORIZED)
                    .message("Token has expired")
                    .data(createValidationResponse(false, null, "Token expired"))
                    .build();
            return ResponseEntity.status(ResponseStatusEnum.UNAUTHORIZED.getHttpStatus()).body(response);
        }

        catch (Exception e) {
            log.error("Error validating token", e);

            GenericResponse<Map<String, Object>> response = GenericResponse.<Map<String, Object>>builder()
                    .status(ResponseStatusEnum.UNAUTHORIZED)
                    .message("Token validation failed")
                    .data(createValidationResponse(false, null, e.getMessage()))
                    .debugMessage(e.getMessage())
                    .build();

            return ResponseEntity.status(ResponseStatusEnum.UNAUTHORIZED.getHttpStatus()).body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<GenericResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("service", "auth-service");
        healthData.put("status", "UP");
        healthData.put("timestamp", System.currentTimeMillis());

        GenericResponse<Map<String, Object>> response = GenericResponse.<Map<String, Object>>builder()
                .status(ResponseStatusEnum.SUCCESS)
                .message("Auth service is running")
                .data(healthData)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user")
    public ResponseEntity<GenericResponse<Map<String, String>>> getCurrentUser(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                GenericResponse<Map<String, String>> response = GenericResponse.<Map<String, String>>builder()
                        .status(ResponseStatusEnum.BAD_REQUEST)
                        .message("Missing or invalid Authorization header")
                        .build();
                return ResponseEntity.status(ResponseStatusEnum.BAD_REQUEST.getHttpStatus()).body(response);
            }

            String token = authHeader.substring(7);

            if (!jwtUtil.isTokenValid(token)) {
                GenericResponse<Map<String, String>> response = GenericResponse.<Map<String, String>>builder()
                        .status(ResponseStatusEnum.UNAUTHORIZED)
                        .message("Token has expired or is invalid")
                        .build();
                return ResponseEntity.status(ResponseStatusEnum.UNAUTHORIZED.getHttpStatus()).body(response);
            }

            String username = jwtUtil.extractUsername(token);

            Map<String, String> userData = new HashMap<>();
            userData.put("username", username);
            userData.put("tokenExpiry", jwtUtil.extractExpiration(token).toString());

            GenericResponse<Map<String, String>> response = GenericResponse.<Map<String, String>>builder()
                    .status(ResponseStatusEnum.SUCCESS)
                    .message("User information retrieved successfully")
                    .data(userData)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving current user", e);

            GenericResponse<Map<String, String>> response = GenericResponse.<Map<String, String>>builder()
                    .status(ResponseStatusEnum.ERROR)
                    .message("Could not retrieve user information")
                    .debugMessage(e.getMessage())
                    .build();

            return ResponseEntity.status(ResponseStatusEnum.ERROR.getHttpStatus()).body(response);
        }
    }

    private Map<String, Object> createValidationResponse(boolean valid, String username, String error) {
        Map<String, Object> validationData = new HashMap<>();
        validationData.put("valid", valid);
        validationData.put("username", username);
        if (error != null) {
            validationData.put("error", error);
        }
        return validationData;
    }
}