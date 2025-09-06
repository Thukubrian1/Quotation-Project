package com.shared.sharedlib.Dtos;

import com.shared.sharedlib.Enums.ResponseStatusEnum;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    public static <T> GenericResponse<T> success(String message, T data) {
        return GenericResponse.<T>builder()
                .status(ResponseStatusEnum.SUCCESS)
                .message(message)
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
}