package com.chat.message.application;

import com.chat.message.dto.MarkReadRequest;

import java.util.UUID;

public interface ReadCursorUpdater {

    /**
     * 예외: R001(방 없음), M001(메시지 없음 — 해당 방의 메시지 아님)
     */
    void markRead(String userId, UUID roomId, MarkReadRequest request);
}
