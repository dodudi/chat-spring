package com.chat.invitation.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.invitation.domain.Invitation;
import com.chat.invitation.dto.InvitationResponse;
import com.chat.invitation.infrastructure.InvitationRepository;
import com.chat.room.domain.ChatRoom;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import com.chat.user.infrastructure.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultInvitationSenderTest {

    @InjectMocks
    private DefaultInvitationSender invitationSender;

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private InvitationRepository invitationRepository;
    @Mock private InvitationNotificationPublisher notificationPublisher;

    @Test
    @DisplayName("초대 발송 성공 — afterCommit 발행 등록")
    void sendInvitation_성공() {
        // given
        String inviterId = "owner";
        String inviteeId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup(inviterId, "그룹방", "key");
        Invitation invitation = Invitation.create(roomId, inviterId, inviteeId);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, inviterId)).willReturn(true);
        given(userRepository.existsById(inviteeId)).willReturn(true);
        given(chatRoomMemberRepository.existsActiveMember(roomId, inviteeId)).willReturn(false);
        given(invitationRepository.existsPending(roomId, inviteeId)).willReturn(false);
        given(invitationRepository.save(any(Invitation.class))).willReturn(invitation);

        try (MockedStatic<TransactionSynchronizationManager> txManager =
                     mockStatic(TransactionSynchronizationManager.class)) {
            txManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                    .thenAnswer(inv -> null);

            // when
            invitationSender.sendInvitation(inviterId, roomId, inviteeId);

            // then
            verify(invitationRepository).save(any(Invitation.class));
            txManager.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()));
        }
    }

    @Test
    @DisplayName("존재하지 않는 방에 초대 시 예외 발생")
    void sendInvitation_방없음_예외() {
        // given
        UUID roomId = UUID.randomUUID();
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> invitationSender.sendInvitation("owner", roomId, "user-1"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("DM 방 초대 시 예외 발생")
    void sendInvitation_DM방_예외() {
        // given
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createDm("owner", "dm-key");
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));

        // when & then
        assertThatThrownBy(() -> invitationSender.sendInvitation("owner", roomId, "user-1"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_TYPE_UNSUPPORTED);
    }

    @Test
    @DisplayName("방장이 아닌 사용자가 초대 시 예외 발생")
    void sendInvitation_방장아님_예외() {
        // given
        String inviterId = "non-owner";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, inviterId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> invitationSender.sendInvitation(inviterId, roomId, "user-1"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_FORBIDDEN);
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 초대 시 예외 발생")
    void sendInvitation_사용자없음_예외() {
        // given
        String inviterId = "owner";
        String inviteeId = "ghost";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup(inviterId, "그룹방", "key");
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, inviterId)).willReturn(true);
        given(userRepository.existsById(inviteeId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> invitationSender.sendInvitation(inviterId, roomId, inviteeId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 멤버인 사용자 초대 시 예외 발생")
    void sendInvitation_이미멤버_예외() {
        // given
        String inviterId = "owner";
        String inviteeId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup(inviterId, "그룹방", "key");
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, inviterId)).willReturn(true);
        given(userRepository.existsById(inviteeId)).willReturn(true);
        given(chatRoomMemberRepository.existsActiveMember(roomId, inviteeId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> invitationSender.sendInvitation(inviterId, roomId, inviteeId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVITATION_ALREADY_MEMBER);
    }

    @Test
    @DisplayName("이미 대기 중인 초대가 있을 시 예외 발생")
    void sendInvitation_중복초대_예외() {
        // given
        String inviterId = "owner";
        String inviteeId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup(inviterId, "그룹방", "key");
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, inviterId)).willReturn(true);
        given(userRepository.existsById(inviteeId)).willReturn(true);
        given(chatRoomMemberRepository.existsActiveMember(roomId, inviteeId)).willReturn(false);
        given(invitationRepository.existsPending(roomId, inviteeId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> invitationSender.sendInvitation(inviterId, roomId, inviteeId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVITATION_DUPLICATE);
    }
}
