package com.shared.sharedlib.Exceptions;
import lombok.Getter;
import java.util.Map;


@Getter
public class ValidationException extends RuntimeException {
    private final Map<String, String> validationErrors;

    public ValidationException(String message, Map<String, String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public ValidationException(String message) {
        super(message);
        this.validationErrors = null;
    }
}