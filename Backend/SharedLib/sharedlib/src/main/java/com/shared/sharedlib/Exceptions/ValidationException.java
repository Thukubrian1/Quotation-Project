package com.shared.sharedlib.Exceptions;

import com.shared.sharedlib.Enums.ResponseStatusEnum;
import lombok.Getter;

import java.util.Map;

@Getter
public class ValidationException extends RuntimeException {
    private final ResponseStatusEnum status;
    private final String debugMessage;
    private final Map<String, String> validationErrors;

    public ValidationException(String message, Map<String, String> validationErrors) {
        super(message);
        this.status = ResponseStatusEnum.VALIDATION_ERROR;
        this.validationErrors = validationErrors;
        this.debugMessage = "Custom validation failed";
    }

    public ValidationException(String message, Map<String, String> validationErrors, String debugMessage) {
        super(message);
        this.status = ResponseStatusEnum.VALIDATION_ERROR;
        this.validationErrors = validationErrors;
        this.debugMessage = debugMessage;
    }

    public ValidationException(String message, Map<String, String> validationErrors, Throwable cause) {
        super(message, cause);
        this.status = ResponseStatusEnum.VALIDATION_ERROR;
        this.validationErrors = validationErrors;
        this.debugMessage = cause.getMessage();
    }
}