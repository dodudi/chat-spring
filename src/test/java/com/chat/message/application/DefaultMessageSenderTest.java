package com.chat.message.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.message.domain.Message;
import com.chat.message.dto.MessageResponse;
import com.chat.message.dto.SendMessageRequest;
import com.chat.message.infrastructure.MessageRepository;
import com.chat.profile.domain.Profile;
import com.chat.profile.infrastructure.ProfileRepository;
import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.domain.MemberRole;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultMessageSenderTest {

    @InjectMocks
    private DefaultMessageSender messageSender;

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private ChatMessagePublisher chatMessagePublisher;

    @Test
    @DisplayName("메시지 전송 성공 — content·senderId 검증")
    void sendMessage_success() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");
        ChatRoomMember member = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);
        Message message = Message.create(roomId, userId, 1L, "안녕하세요");
        Profile profile = Profile.create(userId, "닉네임");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));
        given(messageRepository.save(any(Message.class))).willReturn(message);
        given(profileRepository.findById(1L)).willReturn(Optional.of(profile));

        try (MockedStatic<TransactionSynchronizationManager> txManager =
                     mockStatic(TransactionSynchronizationManager.class)) {
            txManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                    .thenAnswer(inv -> null);

            // when
            MessageResponse response = messageSender.sendMessage(userId, roomId, new SendMessageRequest("안녕하세요"));

            // then
            assertThat(response.content()).isEqualTo("안녕하세요");
            assertThat(response.senderId()).isEqualTo(userId);
            assertThat(response.senderNickname()).isEqualTo("닉네임");
            verify(messageRepository).save(any(Message.class));
        }
    }

    @Test
    @DisplayName("존재하지 않는 방에 메시지 전송 시 예외 발생")
    void sendMessage_roomNotFound_throwsException() {
        // given
        UUID roomId = UUID.randomUUID();
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> messageSender.sendMessage("user-1", roomId, new SendMessageRequest("안녕")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("채팅방 멤버가 아닌 사용자가 메시지 전송 시 예외 발생")
    void sendMessage_nonMember_throwsException() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> messageSender.sendMessage(userId, roomId, new SendMessageRequest("안녕")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("DM 방 메시지 전송 시 양측 숨김 해제")
    void sendMessage_dmRoom_unhidesAll() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createDm(userId, "dm-key");
        ChatRoomMember member = ChatRoomMember.create(roomId, userId, 1L, MemberRole.OWNER);
        Message message = Message.create(roomId, userId, 1L, "안녕");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));
        given(messageRepository.save(any(Message.class))).willReturn(message);
        given(profileRepository.findById(1L)).willReturn(Optional.of(Profile.create(userId, "닉네임")));

        try (MockedStatic<TransactionSynchronizationManager> txManager =
                     mockStatic(TransactionSynchronizationManager.class)) {
            txManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                    .thenAnswer(inv -> null);

            // when
            messageSender.sendMessage(userId, roomId, new SendMessageRequest("안녕"));

            // then
            verify(chatRoomMemberRepository).unhideAll(roomId);
        }
    }
}
