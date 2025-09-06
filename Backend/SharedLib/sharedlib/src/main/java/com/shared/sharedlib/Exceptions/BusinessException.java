package com.shared.sharedlib.Exceptions;


import com.shared.sharedlib.Enums.ResponseStatusEnum;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ResponseStatusEnum status;
    private final String debugMessage;

    public BusinessException(ResponseStatusEnum status, String message) {
        super(message);
        this.status = status;
        this.debugMessage = null;
    }

    public BusinessException(ResponseStatusEnum status, String message, String debugMessage) {
        super(message);
        this.status = status;
        this.debugMessage = debugMessage;
    }

    public BusinessException(ResponseStatusEnum status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.debugMessage = cause.getMessage();
    }
}




