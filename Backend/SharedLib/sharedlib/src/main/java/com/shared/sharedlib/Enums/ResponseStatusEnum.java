package com.shared.sharedlib.Enums;

import org.springframework.http.HttpStatus;

public enum ResponseStatusEnum {

    SUCCESS(HttpStatus.OK),
    ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(HttpStatus.UNPROCESSABLE_ENTITY);

    private final HttpStatus httpStatus;

    ResponseStatusEnum(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}