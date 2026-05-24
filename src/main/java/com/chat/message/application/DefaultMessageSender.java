package com.chat.message.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.message.domain.Message;
import com.chat.message.dto.MessageResponse;
import com.chat.message.dto.SendMessageRequest;
import com.chat.message.infrastructure.MessageRepository;
import com.chat.profile.domain.Profile;
import com.chat.profile.infrastructure.ProfileRepository;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.domain.RoomType;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DefaultMessageSender implements MessageSender {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MessageRepository messageRepository;
    private final ProfileRepository profileRepository;
    private final ChatMessagePublisher chatMessagePublisher;

    @Override
    public MessageResponse sendMessage(String userId, UUID roomId, SendMessageRequest request) {
        var room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        ChatRoomMember member = chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .filter(m -> m.getLeftAt() == null && m.getKickedAt() == null)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_MEMBER_NOT_FOUND));

        // DM 숨김 해제 — 메시지 전송 시 양측 DM이 목록에 다시 노출
        if (room.getType() == RoomType.DM) {
            chatRoomMemberRepository.unhideAll(roomId);
        }

        Message message = Message.create(roomId, userId, member.getProfileId(), request.content());
        messageRepository.save(message);

        String nickname = member.getProfileId() == null ? "" :
                profileRepository.findById(member.getProfileId())
                        .map(Profile::getNickname).orElse("");

        MessageResponse response = MessageResponse.of(message, nickname);
        // 트랜잭션 커밋 후 발행 — 롤백 시 유령 메시지 방지
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chatMessagePublisher.publishToRoom(roomId, response);
            }
        });
        return response;
    }
}
