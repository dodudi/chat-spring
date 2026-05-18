package com.chat.message.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.message.domain.Message;
import com.chat.message.dto.MessageCursorResponse;
import com.chat.message.dto.MessageResponse;
import com.chat.message.infrastructure.MessageRepository;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final EntityManager entityManager;

    @Override
    public MessageCursorResponse getMessages(String userId, Long roomId, Long before, int size) {
        if (!chatRoomMemberRepository.existsByRoom_IdAndUserId(roomId, userId)) {
            throw new AppException(ErrorCode.ROOM_ACCESS_DENIED);
        }

        PageRequest pageable = PageRequest.of(0, size + 1);
        List<Message> fetched = before != null
                ? messageRepository.findByRoomIdAndIdLessThanOrderByIdDesc(roomId, before, pageable)
                : messageRepository.findByRoomIdOrderByIdDesc(roomId, pageable);

        boolean hasMore = fetched.size() > size;
        List<Message> page = hasMore ? fetched.subList(0, size) : fetched;
        Long nextCursor = hasMore ? page.get(page.size() - 1).getId() : null;

        return new MessageCursorResponse(
                page.stream().map(MessageResponse::from).toList(),
                nextCursor,
                hasMore
        );
    }

    @Override
    @Transactional
    public void deleteMessage(String userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
        if (!message.isOwnedBy(userId)) {
            throw new AppException(ErrorCode.MESSAGE_DELETE_DENIED);
        }
        message.delete();
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    @Transactional
    public Message sendMessage(String userId, Long roomId, String content) {
        if (!chatRoomMemberRepository.existsByRoom_IdAndUserId(roomId, userId)) {
            throw new AppException(ErrorCode.ROOM_ACCESS_DENIED);
        }
        Message message = messageRepository.save(Message.create(roomId, userId, content));
        chatRoomRepository.findById(roomId).ifPresent(room -> room.touch());
        return message;
    }

    @Override
    @Transactional
    public void markRead(String userId, Long roomId, Long messageId) {
        ChatRoomMember member = chatRoomMemberRepository.findByRoom_IdAndUserId(roomId, userId)
                .filter(ChatRoomMember::isActive)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_ACCESS_DENIED));
        member.updateLastRead(messageId);
    }
}
