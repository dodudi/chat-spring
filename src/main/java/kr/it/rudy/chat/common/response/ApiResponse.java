package kr.it.rudy.chat.common.response;

import kr.it.rudy.chat.common.exception.ErrorCode;

public record ApiResponse<T>(String code, String message, T data) {

    private static final String SUCCESS_CODE = "200";
    private static final String SUCCESS_MESSAGE = "OK";

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null);
    }
}
