package com.shared.sharedlib.Exceptions;

import com.shared.sharedlib.Dtos.GenericResponse;
import com.shared.sharedlib.Enums.ResponseStatusEnum;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GenericResponse<Object>> handleBusinessException(BusinessException ex) {
        log.error("Business exception occurred: {}", ex.getMessage(), ex);

        GenericResponse<Object> response = GenericResponse.builder()
                .status(ex.getStatus())
                .message(ex.getMessage())
                .debugMessage(ex.getDebugMessage())
                .build();

        return ResponseEntity.status(ex.getStatus().getHttpStatus()).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<GenericResponse<Object>> handleValidationException(ValidationException ex) {
        log.error("Validation exception occurred: {}", ex.getMessage(), ex);

        GenericResponse<Object> response = GenericResponse.builder()
                .status(ResponseStatusEnum.VALIDATION_ERROR)
                .message(ex.getMessage())
                .data(ex.getValidationErrors())
                .debugMessage("Custom validation failed")
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<GenericResponse<Object>> handleAuthenticationException(AuthenticationException ex) {
        log.error("Authentication exception: {}", ex.getMessage(), ex);

        GenericResponse<Object> response = GenericResponse.builder()
                .status(ResponseStatusEnum.UNAUTHORIZED)
                .message("Authentication failed")
                .debugMessage(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GenericResponse<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access denied exception: {}", ex.getMessage(), ex);

        GenericResponse<Object> response = GenericResponse.builder()
                .status(ResponseStatusEnum.FORBIDDEN)
                .message("Access denied")
                .debugMessage(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Validation exception: {}", ex.getMessage(), ex);

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        GenericResponse<Object> response = GenericResponse.builder()
                .status(ResponseStatusEnum.VALIDATION_ERROR)
                .message("Validation failed")
                .data(errors)
                .debugMessage("Field validation errors occurred")
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse<Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        log.error("Constraint violation exception: {}", ex.getMessage(), ex);

        Map<String, String> errors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));

        GenericResponse<Object> response = GenericResponse.builder()
                .status(ResponseStatusEnum.VALIDATION_ERROR)
                .message("Validation constraints violated")
                .data(errors)
                .debugMessage(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.error("HTTP message not readable exception: {}", ex.getMessage(), ex);

        GenericResponse<Object> response = GenericResponse.builder()
                .status(ResponseStatusEnum.BAD_REQUEST)
                .message("Invalid request format")
                .debugMessage("Request body is malformed or missing")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<GenericResponse<Object>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.error("Missing request parameter exception: {}", ex.getMessage(), ex);

        GenericResponse<Object> response = GenericResponse.builder()
                .status(ResponseStatusEnum.BAD_REQUEST)
                .message("Missing required parameter: " + ex.getParameterName())
                .debugMessage(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GenericResponse<Object>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.error("Method argument type mismatch exception: {}", ex.getMessage(), ex);

        GenericResponse<Object> response = GenericResponse.builder()
                .status(ResponseStatusEnum.BAD_REQUEST)
                .message("Invalid parameter type for: " + ex.getName())
                .debugMessage(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GenericResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Illegal argument exception: {}", ex.getMessage(), ex);

        GenericResponse<Object> response = GenericResponse.builder()
                .status(ResponseStatusEnum.BAD_REQUEST)
                .message("Invalid argument provided")
                .debugMessage(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<Object>> handleGenericException(Exception ex) {
        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);

        GenericResponse<Object> response = GenericResponse.builder()
                .status(ResponseStatusEnum.ERROR)
                .message("An unexpected error occurred")
                .debugMessage("Internal server error")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}