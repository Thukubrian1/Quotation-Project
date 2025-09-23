package com.userservice.userservice.Controller;

import com.shared.sharedlib.Dtos.GenericResponse;
import com.shared.sharedlib.Enums.ResponseStatusEnum;
import com.userservice.userservice.Dtos.UserDto;
import com.userservice.userservice.ServiceImpl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/users")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class UserController {

    private final UserServiceImpl userService;

    @GetMapping("/getuser")
    public ResponseEntity<GenericResponse<UserDto>> getUser(@RequestParam(name = "userName", required = true) String userName){
        try {
            log.debug("Received request to get user with userName: '{}'", userName);

            String cleanUserName = userName.trim();

            if (cleanUserName.isEmpty()) {
                GenericResponse<UserDto> errorResponse = GenericResponse.<UserDto>builder()
                        .status(ResponseStatusEnum.BAD_REQUEST)
                        .message("Username parameter cannot be empty")
                        .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }

            GenericResponse<UserDto> response = userService.getUserByUsername(cleanUserName);

            return switch (response.getStatus()) {
                case SUCCESS -> ResponseEntity.ok(response);
                case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            };

        }
        catch (Exception e) {
            log.error("Unexpected error in getUser: {}", e.getMessage(), e);
            GenericResponse<UserDto> errorResponse = GenericResponse.<UserDto>builder()
                    .status(ResponseStatusEnum.ERROR)
                    .message("An unexpected error occurred")
                    .debugMessage(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/adduser")
    public ResponseEntity<GenericResponse<UserDto>> createUser(@RequestBody UserDto userDto) {
        try {
            log.info("Creating user with email: {}", userDto.getUserEmail());

            // Basic validation
            if (userDto.getUserEmail() == null || userDto.getUserEmail().trim().isEmpty()) {
                GenericResponse<UserDto> errorResponse = GenericResponse.<UserDto>builder()
                        .status(ResponseStatusEnum.BAD_REQUEST)
                        .message("User email is required")
                        .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (userDto.getUserName() == null || userDto.getUserName().trim().isEmpty()) {
                GenericResponse<UserDto> errorResponse = GenericResponse.<UserDto>builder()
                        .status(ResponseStatusEnum.BAD_REQUEST)
                        .message("Username is required")
                        .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (userDto.getUserPassword() == null || userDto.getUserPassword().length() < 6) {
                GenericResponse<UserDto> errorResponse = GenericResponse.<UserDto>builder()
                        .status(ResponseStatusEnum.BAD_REQUEST)
                        .message("Password must be at least 6 characters long")
                        .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String userEmail = userDto.getUserEmail();
            GenericResponse<UserDto> response = userService.addUser(userDto, userEmail);

            return switch (response.getStatus()) {
                case SUCCESS -> ResponseEntity.status(HttpStatus.CREATED).body(response);
                case CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                case BAD_REQUEST -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            };
        }
        catch (Exception e) {
            log.error("Unexpected error in createUser: {}", e.getMessage(), e);
            GenericResponse<UserDto> errorResponse = GenericResponse.<UserDto>builder()
                    .status(ResponseStatusEnum.ERROR)
                    .message("An unexpected error occurred while creating user")
                    .debugMessage(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/login")
    public ResponseEntity<GenericResponse<UserDto>> login(
            @RequestParam String userEmail,
            @RequestParam String userPassword) {
        try{
            log.info("Login attempt for email: {}", userEmail);

            String cleanUserEmail = userEmail.trim();
            if (cleanUserEmail.isEmpty()) {
                GenericResponse<UserDto> errorResponse = GenericResponse.<UserDto>builder()
                        .status(ResponseStatusEnum.BAD_REQUEST)
                        .message("User email parameter cannot be empty")
                        .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String cleanUserPassword = userPassword.trim();
            if (cleanUserPassword.isEmpty()) {
                GenericResponse<UserDto> errorResponse = GenericResponse.<UserDto>builder()
                        .status(ResponseStatusEnum.BAD_REQUEST)
                        .message("User password parameter cannot be empty")
                        .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }

            GenericResponse<UserDto> response = userService.loginUser(cleanUserEmail, cleanUserPassword);

            return switch (response.getStatus()) {
                case SUCCESS -> {
                    log.info("Successful login for email: {}", cleanUserEmail);
                    yield ResponseEntity.ok(response);
                }
                case UNAUTHORIZED -> {
                    log.warn("Unauthorized login attempt for email: {}", cleanUserEmail);
                    yield ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                }
                case NOT_FOUND -> {
                    log.warn("User not found for email: {}", cleanUserEmail);
                    yield ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response); // Don't reveal user existence
                }
                default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            };
        }
        catch (Exception e) {
            log.error("Unexpected error in login for email: {}", userEmail, e);
            GenericResponse<UserDto> errorResponse = GenericResponse.<UserDto>builder()
                    .status(ResponseStatusEnum.ERROR)
                    .message("An unexpected error occurred during login")
                    .debugMessage(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Add test endpoint - NO AUTHENTICATION REQUIRED
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        try {
            log.info("Test endpoint called");
            Map<String, Object> response = new HashMap<>();
            response.put("service", "user-service");
            response.put("message", "User service is running");
            response.put("timestamp", System.currentTimeMillis());
            response.put("status", "UP");
            response.put("version", "1.0.0");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in test endpoint: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "user-service");
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<GenericResponse<Map<String, Object>>> healthCheck() {
        try {
            log.debug("Health check endpoint called");

            Map<String, Object> healthData = new HashMap<>();
            healthData.put("service", "user-service");
            healthData.put("status", "UP");
            healthData.put("timestamp", System.currentTimeMillis());
            healthData.put("port", 8085);
            healthData.put("environment", "development");

            // Add some basic system info
            healthData.put("memory", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            healthData.put("processors", Runtime.getRuntime().availableProcessors());

            GenericResponse<Map<String, Object>> response = GenericResponse.<Map<String, Object>>builder()
                    .status(ResponseStatusEnum.SUCCESS)
                    .message("User service is running normally")
                    .data(healthData)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in health check: {}", e.getMessage(), e);

            Map<String, Object> healthData = new HashMap<>();
            healthData.put("service", "user-service");
            healthData.put("status", "ERROR");
            healthData.put("timestamp", System.currentTimeMillis());
            healthData.put("error", e.getMessage());

            GenericResponse<Map<String, Object>> response = GenericResponse.<Map<String, Object>>builder()
                    .status(ResponseStatusEnum.ERROR)
                    .message("Service health check failed")
                    .data(healthData)
                    .debugMessage(e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        log.debug("Ping endpoint called");
        return ResponseEntity.ok("pong");
    }

    @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-email")
    public ResponseEntity<GenericResponse<Boolean>> checkEmailExists(@RequestParam String email) {
        try {
            log.debug("Checking if email exists: {}", email);

            if (email == null || email.trim().isEmpty()) {
                GenericResponse<Boolean> errorResponse = GenericResponse.<Boolean>builder()
                        .status(ResponseStatusEnum.BAD_REQUEST)
                        .message("Email parameter is required")
                        .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }


            GenericResponse<Boolean> response = GenericResponse.<Boolean>builder()
                    .status(ResponseStatusEnum.SUCCESS)
                    .message("Email check completed")
                    .data(false) // Placeholder - implement actual logic in service
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking email existence: {}", e.getMessage(), e);
            GenericResponse<Boolean> errorResponse = GenericResponse.<Boolean>builder()
                    .status(ResponseStatusEnum.ERROR)
                    .message("Error checking email existence")
                    .debugMessage(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}