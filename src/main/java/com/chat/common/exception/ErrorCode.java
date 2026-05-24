package com.chat.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST,             "C001", "요청 값 유효성 오류"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,             "C002", "인증 실패"),
    FORBIDDEN(HttpStatus.FORBIDDEN,                   "C003", "권한 없음"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND,          "C004", "리소스 없음"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,  "C005", "서버 내부 오류"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,              "U001", "대상 사용자 없음"),

    // Profile
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND,           "P001", "프로필 없음"),
    PROFILE_FORBIDDEN(HttpStatus.FORBIDDEN,           "P002", "본인 프로필이 아님"),
    PROFILE_IN_USE(HttpStatus.CONFLICT,               "P003", "채팅방에서 사용 중인 프로필"),

    // Room
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND,              "R001", "채팅방 없음"),
    ROOM_ALREADY_JOINED(HttpStatus.CONFLICT,          "R003", "이미 참여 중"),
    ROOM_MEMBER_LIMIT(HttpStatus.CONFLICT,            "R004", "인원 초과"),
    ROOM_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST,    "R005", "비밀번호 불일치"),
    ROOM_KICKED(HttpStatus.FORBIDDEN,                 "R006", "강퇴된 사용자"),
    ROOM_FORBIDDEN(HttpStatus.FORBIDDEN,              "R007", "방장 권한 없음"),
    ROOM_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND,       "R008", "대상 멤버 없음"),
    ROOM_EMPTY(HttpStatus.NOT_FOUND,                  "R009", "빈 방 — 참여 불가"),
    ROOM_TYPE_UNSUPPORTED(HttpStatus.FORBIDDEN,       "R010", "지원하지 않는 채팅방 타입"),

    // Invitation
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND,        "I001", "초대 없음"),
    INVITATION_ALREADY_PROCESSED(HttpStatus.CONFLICT, "I002", "이미 처리된 초대"),
    INVITATION_ALREADY_MEMBER(HttpStatus.CONFLICT,    "I003", "이미 참여 중인 사용자"),
    INVITATION_DUPLICATE(HttpStatus.CONFLICT,         "I004", "PENDING 상태의 초대 이미 존재"),

    // InviteLink
    INVITE_LINK_EXPIRED(HttpStatus.GONE,              "L001", "만료된 초대 링크"),
    INVITE_LINK_INACTIVE(HttpStatus.GONE,             "L002", "비활성화된 초대 링크"),

    // Message
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND,           "M001", "메시지 없음"),
    MESSAGE_FORBIDDEN(HttpStatus.FORBIDDEN,           "M002", "본인 메시지가 아님"),

    // Group
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND,             "G001", "그룹 없음"),
    GROUP_NAME_DUPLICATE(HttpStatus.CONFLICT,         "G002", "그룹 이름 중복"),
    GROUP_LIMIT_EXCEEDED(HttpStatus.CONFLICT,         "G003", "최대 그룹 수 초과"),
    GROUP_DEFAULT_IMMUTABLE(HttpStatus.FORBIDDEN,     "G004", "기본 그룹 변경 불가"),
    GROUP_ROOM_ALREADY_ASSIGNED(HttpStatus.CONFLICT,  "G005", "이미 그룹에 할당된 채팅방");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
