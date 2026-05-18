package com.chat.websocket.dto;

import com.chat.common.exception.AppException;

public record ErrorMessage(
        String code,
        String message
) {

    public static ErrorMessage from(AppException ex) {
        return new ErrorMessage(
                ex.getErrorCode().getCode(),
                ex.getErrorCode().getMessage()
        );
    }

    public static ErrorMessage of(String code, String message) {
        return new ErrorMessage(code, message);
    }
}
