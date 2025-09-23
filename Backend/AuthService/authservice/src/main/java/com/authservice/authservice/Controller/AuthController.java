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
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}) // Add CORS at controller level
public class AuthController {

    private final AuthService authService;
    private final JWTUtil jwtUtil;

    // Accept both form data and JSON
    @PostMapping("/login")
    public ResponseEntity<GenericResponse<Map<String, String>>> login(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String userEmail,
            @RequestBody(required = false) Map<String, String> loginRequest) {

        try {
            // Handle both form data and JSON body
            String user = userName;
            String email = userEmail;

            if (loginRequest != null && !loginRequest.isEmpty()) {
                user = loginRequest.get("username");
                email = loginRequest.get("email");
            }

            log.info("Login attempt for username: {}", user);

            // Validate input parameters
            if (!StringUtils.hasText(user) || !StringUtils.hasText(email)) {
                log.warn("Missing username or password in login request");
                GenericResponse<Map<String, String>> response = GenericResponse.<Map<String, String>>builder()
                        .status(ResponseStatusEnum.BAD_REQUEST)
                        .message("Username and password are required")
                        .build();
                return ResponseEntity.status(400).body(response);
            }

            // Attempt login
            String token = authService.login(user.trim(), email.trim());

            // Prepare response data
            Map<String, String> tokenData = new HashMap<>();
            tokenData.put("access_token", token);
            tokenData.put("token_type", "Bearer");
            tokenData.put("username", user.trim());

            GenericResponse<Map<String, String>> response = GenericResponse.<Map<String, String>>builder()
                    .status(ResponseStatusEnum.SUCCESS)
                    .message("Login successful")
                    .data(tokenData)
                    .build();

            log.info("Successful login for username: {}", user);
            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            log.warn("Business exception during login: {}", e.getMessage());

            GenericResponse<Map<String, String>> response = GenericResponse.<Map<String, String>>builder()
                    .status(e.getStatus())
                    .message(e.getMessage())
                    .debugMessage(e.getDebugMessage())
                    .build();

            return ResponseEntity.status(401).body(response);

        } catch (Exception e) {
            log.error("Unexpected error during login", e);

            GenericResponse<Map<String, String>> response = GenericResponse.<Map<String, String>>builder()
                    .status(ResponseStatusEnum.ERROR)
                    .message("An unexpected error occurred")
                    .debugMessage(e.getMessage())
                    .build();

            return ResponseEntity.status(500).body(response);
        }
    }

    // Add a simple test endpoint
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Auth service is running");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(response);
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
                return ResponseEntity.status(400).body(response);
            }

            String token = authHeader.substring(7);

            if (!StringUtils.hasText(token)) {
                GenericResponse<Map<String, Object>> response = GenericResponse.<Map<String, Object>>builder()
                        .status(ResponseStatusEnum.BAD_REQUEST)
                        .message("Token is required")
                        .data(createValidationResponse(false, null, "Empty token"))
                        .build();
                return ResponseEntity.status(400).body(response);
            }

            // Validate token and extract username
            if (!jwtUtil.isTokenValid(token)) {
                GenericResponse<Map<String, Object>> response = GenericResponse.<Map<String, Object>>builder()
                        .status(ResponseStatusEnum.UNAUTHORIZED)
                        .message("Token has expired or is invalid")
                        .data(createValidationResponse(false, null, "Token expired or invalid"))
                        .build();
                return ResponseEntity.status(401).body(response);
            }

            String username = jwtUtil.extractUsername(token);

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
            return ResponseEntity.status(401).body(response);
        }

        catch (Exception e) {
            log.error("Error validating token", e);

            GenericResponse<Map<String, Object>> response = GenericResponse.<Map<String, Object>>builder()
                    .status(ResponseStatusEnum.UNAUTHORIZED)
                    .message("Token validation failed")
                    .data(createValidationResponse(false, null, e.getMessage()))
                    .debugMessage(e.getMessage())
                    .build();

            return ResponseEntity.status(401).body(response);
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