package com.chat.message.application;

import com.chat.message.dto.MessageCursorResponse;

import java.util.UUID;

public interface MessageReader {

    /**
     * 커서 기반 메시지 히스토리 조회 (내림차순).
     * DM 숨김 처리된 사용자는 hidden_at 이후 메시지만 조회.
     *
     * 예외: R001(방 없음), R008(비활성 멤버)
     */
    MessageCursorResponse getHistory(String userId, UUID roomId, Long cursor, int size);
}
