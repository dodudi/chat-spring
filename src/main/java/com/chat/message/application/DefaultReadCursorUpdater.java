package com.chat.message.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.message.domain.ReadCursor;
import com.chat.message.dto.MarkReadRequest;
import com.chat.message.infrastructure.MessageRepository;
import com.chat.message.infrastructure.ReadCursorRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DefaultReadCursorUpdater implements ReadCursorUpdater {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final ReadCursorRepository readCursorRepository;

    @Override
    public void markRead(String userId, UUID roomId, MarkReadRequest request) {
        chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        messageRepository.findById(request.lastReadMessageId())
                .filter(m -> m.getRoomId().equals(roomId))
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        ReadCursor cursor = readCursorRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseGet(() -> ReadCursor.create(roomId, userId, request.lastReadMessageId()));
        cursor.update(request.lastReadMessageId());
        readCursorRepository.save(cursor);
    }
}
