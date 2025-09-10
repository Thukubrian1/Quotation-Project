package com.shared.sharedlib.Dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shared.sharedlib.Enums.ResponseStatusEnum;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericResponse<T> {

    private ResponseStatusEnum status;
    private String message;
    private String debugMessage;
    private T data;

    // Convenience methods for common responses
    public static <T> GenericResponse<T> success(T data) {
        return GenericResponse.<T>builder()
                .status(ResponseStatusEnum.SUCCESS)
                .message("Operation successful")
                .data(data)
                .build();
    }

    public static <T> GenericResponse<T> error(String message) {
        return GenericResponse.<T>builder()
                .status(ResponseStatusEnum.ERROR)
                .message(message)
                .build();
    }

    public static <T> GenericResponse<T> error(String message, String debugMessage) {
        return GenericResponse.<T>builder()
                .status(ResponseStatusEnum.ERROR)
                .message(message)
                .debugMessage(debugMessage)
                .build();
    }

    public static <T> GenericResponse<T> notFound(String message) {
        return GenericResponse.<T>builder()
                .status(ResponseStatusEnum.NOT_FOUND)
                .message(message)
                .build();
    }

    public static <T> GenericResponse<T> unauthorized(String message) {
        return GenericResponse.<T>builder()
                .status(ResponseStatusEnum.UNAUTHORIZED)
                .message(message)
                .build();
    }

    public static <T> GenericResponse<T> forbidden(String message) {
        return GenericResponse.<T>builder()
                .status(ResponseStatusEnum.FORBIDDEN)
                .message(message)
                .build();
    }

    public static <T> GenericResponse<T> validationError(String message, T errors) {
        return GenericResponse.<T>builder()
                .status(ResponseStatusEnum.VALIDATION_ERROR)
                .message(message)
                .data(errors)
                .build();
    }

    public static <T> GenericResponse<T> badRequest(String message) {
        return GenericResponse.<T>builder()
                .status(ResponseStatusEnum.BAD_REQUEST)
                .message(message)
                .build();
    }
}