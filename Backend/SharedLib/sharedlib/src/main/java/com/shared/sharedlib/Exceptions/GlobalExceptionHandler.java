package com.shared.sharedlib.Exceptions;

import com.shared.sharedlib.Dtos.GenericResponse;
import com.shared.sharedlib.Enums.ResponseStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GenericResponse<Object>> handleBusinessException(BusinessException e) {
        log.warn("Business exception occurred: {}", e.getMessage());

        GenericResponse<Object> response = GenericResponse.builder()
                .status(e.getStatus())
                .message(e.getMessage())
                .debugMessage(e.getDebugMessage())
                .build();

        return ResponseEntity.status(e.getStatus().getHttpStatus()).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<GenericResponse<Object>> handleValidationException(ValidationException e) {
        log.warn("Validation exception occurred: {}", e.getMessage());

        GenericResponse<Object> response = GenericResponse.builder()
                .status(ResponseStatusEnum.VALIDATION_ERROR)
                .message(e.getMessage())
                .data(e.getValidationErrors())
                .build();

        return ResponseEntity.status(ResponseStatusEnum.VALIDATION_ERROR.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        GenericResponse<Map<String, String>> response = GenericResponse.<Map<String, String>>builder()
                .status(ResponseStatusEnum.VALIDATION_ERROR)
                .message("Validation failed")
                .data(errors)
                .build();

        return ResponseEntity.status(ResponseStatusEnum.VALIDATION_ERROR.getHttpStatus()).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse<Object>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("Constraint violation: {}", e.getMessage());

        GenericResponse<Object> response = GenericResponse.builder()
                .status(ResponseStatusEnum.VALIDATION_ERROR)
                .message("Validation constraint violated")
                .debugMessage(e.getMessage())
                .build();

        return ResponseEntity.status(ResponseStatusEnum.VALIDATION_ERROR.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GenericResponse<Object>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch exception: {}", e.getMessage());

        String message = String.format("Invalid value '%s' for parameter '%s'", e.getValue(), e.getName());

        GenericResponse<Object> response = GenericResponse.builder()
                .status(ResponseStatusEnum.BAD_REQUEST)
                .message(message)
                .debugMessage(e.getMessage())
                .build();

        return ResponseEntity.status(ResponseStatusEnum.BAD_REQUEST.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<Object>> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);

        GenericResponse<Object> response = GenericResponse.builder()
                .status(ResponseStatusEnum.ERROR)
                .message("An unexpected error occurred")
                .debugMessage(e.getMessage())
                .build();

        return ResponseEntity.status(ResponseStatusEnum.ERROR.getHttpStatus()).body(response);
    }
}