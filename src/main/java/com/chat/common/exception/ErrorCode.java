package com.chat.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C004", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C005", "접근 권한이 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),

    // Room
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "채팅방을 찾을 수 없습니다."),
    ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "R002", "채팅방에 접근 권한이 없습니다."),
    DUPLICATE_DM_ROOM(HttpStatus.CONFLICT, "R003", "이미 존재하는 DM 방입니다."),

    // Message
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "메시지를 찾을 수 없습니다."),
    MESSAGE_DELETE_DENIED(HttpStatus.FORBIDDEN, "M002", "본인이 보낸 메시지만 삭제할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
