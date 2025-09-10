package com.shared.sharedlib.Enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ResponseStatusEnum {

    SUCCESS("Success", HttpStatus.OK),
    CREATED("Resource created successfully", HttpStatus.CREATED),
    ACCEPTED("Request accepted", HttpStatus.ACCEPTED),
    NO_CONTENT("No content", HttpStatus.NO_CONTENT),

    BAD_REQUEST("Bad request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("Forbidden", HttpStatus.FORBIDDEN),
    NOT_FOUND("Resource not found", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("Method not allowed", HttpStatus.METHOD_NOT_ALLOWED),
    CONFLICT("Conflict", HttpStatus.CONFLICT),

    VALIDATION_ERROR("Validation error", HttpStatus.UNPROCESSABLE_ENTITY),

    ERROR("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("Service unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    GATEWAY_TIMEOUT("Gateway timeout", HttpStatus.GATEWAY_TIMEOUT);

    private final String message;
    private final HttpStatus httpStatus;

    ResponseStatusEnum(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}