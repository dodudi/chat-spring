package kr.it.rudy.chat.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "서버 오류가 발생했습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C002", "잘못된 입력값입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C003", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C004", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "C005", "요청한 리소스를 찾을 수 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "US001", "사용자를 찾을 수 없습니다."),

    // Channel
    CHANNEL_NOT_FOUND(HttpStatus.NOT_FOUND, "CH001", "채널을 찾을 수 없습니다."),
    CHANNEL_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CH002", "채널 카테고리를 찾을 수 없습니다."),
    CHANNEL_FORBIDDEN(HttpStatus.FORBIDDEN, "CH003", "채널을 수정할 권한이 없습니다."),

    // Server
    SERVER_NOT_FOUND(HttpStatus.NOT_FOUND, "SV001", "서버를 찾을 수 없습니다."),
    SERVER_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "SV002", "서버 멤버를 찾을 수 없습니다."),
    SERVER_ALREADY_JOINED(HttpStatus.CONFLICT, "SV003", "이미 가입한 서버입니다."),
    SERVER_FORBIDDEN(HttpStatus.FORBIDDEN, "SV004", "서버를 수정할 권한이 없습니다."),
    SERVER_OWNER_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "SV005", "서버 소유자는 서버를 탈퇴할 수 없습니다."),
    INVITE_NOT_FOUND(HttpStatus.NOT_FOUND, "SV006", "초대 코드를 찾을 수 없습니다."),
    INVITE_EXPIRED(HttpStatus.BAD_REQUEST, "SV007", "만료된 초대 코드입니다."),
    INVITE_EXHAUSTED(HttpStatus.BAD_REQUEST, "SV008", "사용 횟수가 초과된 초대 코드입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
