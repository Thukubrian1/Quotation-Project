package com.authservice.authservice.Controller;

import com.authservice.authservice.Service.AuthService;
import com.shared.sharedlib.Dtos.GenericResponse;
import com.shared.sharedlib.Enums.ResponseStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<GenericResponse<String>> login(@RequestParam String username,
                                                         @RequestParam String password) {
        try {
            // Validate input parameters
            if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
                GenericResponse<String> response = GenericResponse.<String>builder()
                        .status(ResponseStatusEnum.BAD_REQUEST)
                        .message("Username and password are required")
                        .build();
                return ResponseEntity.status(ResponseStatusEnum.BAD_REQUEST.getHttpStatus()).body(response);
            }

            // Attempt login
            String token = authService.login(username, password);

            GenericResponse<String> response = GenericResponse.<String>builder()
                    .status(ResponseStatusEnum.SUCCESS)
                    .message("Login successful")
                    .data("Bearer Token: " + token)
                    .build();

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Login failed for username: {}", username, e);

            GenericResponse<String> response = GenericResponse.<String>builder()
                    .status(ResponseStatusEnum.UNAUTHORIZED)
                    .message("Invalid credentials")
                    .debugMessage(e.getMessage())
                    .build();

            return ResponseEntity.status(ResponseStatusEnum.UNAUTHORIZED.getHttpStatus()).body(response);

        } catch (Exception e) {
            log.error("Unexpected error during login for username: {}", username, e);

            GenericResponse<String> response = GenericResponse.<String>builder()
                    .status(ResponseStatusEnum.ERROR)
                    .message("An unexpected error occurred")
                    .debugMessage(e.getMessage())
                    .build();

            return ResponseEntity.status(ResponseStatusEnum.ERROR.getHttpStatus()).body(response);
        }
    }

    // Example of using the convenience methods
    @GetMapping("/health")
    public ResponseEntity<GenericResponse<String>> healthCheck() {
        GenericResponse<String> response = GenericResponse.success("Auth service is running");
        return ResponseEntity.ok(response);
    }

}