package com.chat.invitelink.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.invitelink.domain.InviteLink;
import com.chat.invitelink.dto.CreateInviteLinkRequest;
import com.chat.invitelink.dto.InviteLinkResponse;
import com.chat.invitelink.infrastructure.InviteLinkRepository;
import com.chat.room.domain.ChatRoom;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultInviteLinkManagerTest {

    @InjectMocks
    private DefaultInviteLinkManager inviteLinkManager;

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private InviteLinkRepository inviteLinkRepository;

    @Test
    @DisplayName("초대 링크 생성 성공")
    void createLink_성공() {
        // given
        String userId = "owner";
        UUID roomId = UUID.randomUUID();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);
        ChatRoom room = ChatRoom.createGroup(userId, "그룹방", "key");
        InviteLink link = InviteLink.create(roomId, userId, expiresAt);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, userId)).willReturn(true);
        given(inviteLinkRepository.save(any(InviteLink.class))).willReturn(link);

        // when
        InviteLinkResponse response = inviteLinkManager.createLink(userId, roomId, new CreateInviteLinkRequest(expiresAt));

        // then
        assertThat(response).isNotNull();
        verify(inviteLinkRepository).save(any(InviteLink.class));
    }

    @Test
    @DisplayName("존재하지 않는 방에 링크 생성 시 예외 발생")
    void createLink_방없음_예외() {
        // given
        UUID roomId = UUID.randomUUID();
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inviteLinkManager.createLink("owner", roomId, new CreateInviteLinkRequest(null)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("DM 방에 링크 생성 시 예외 발생")
    void createLink_DM방_예외() {
        // given
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createDm("owner", "dm-key");
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));

        // when & then
        assertThatThrownBy(() -> inviteLinkManager.createLink("owner", roomId, new CreateInviteLinkRequest(null)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_TYPE_UNSUPPORTED);
    }

    @Test
    @DisplayName("방장이 아닌 사용자가 링크 생성 시 예외 발생")
    void createLink_방장아님_예외() {
        // given
        String userId = "non-owner";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> inviteLinkManager.createLink(userId, roomId, new CreateInviteLinkRequest(null)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_FORBIDDEN);
    }

    @Test
    @DisplayName("초대 링크 비활성화 성공")
    void deactivateLink_성공() {
        // given
        String userId = "owner";
        UUID roomId = UUID.randomUUID();
        InviteLink link = InviteLink.create(roomId, userId, null);

        given(inviteLinkRepository.findById(1L)).willReturn(Optional.of(link));
        given(chatRoomMemberRepository.isOwner(roomId, userId)).willReturn(true);

        // when
        inviteLinkManager.deactivateLink(userId, 1L);

        // then
        assertThat(link.isActive()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 링크 비활성화 시 예외 발생")
    void deactivateLink_링크없음_예외() {
        // given
        given(inviteLinkRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inviteLinkManager.deactivateLink("owner", 1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("방장이 아닌 사용자가 링크 비활성화 시 예외 발생")
    void deactivateLink_방장아님_예외() {
        // given
        String userId = "non-owner";
        UUID roomId = UUID.randomUUID();
        InviteLink link = InviteLink.create(roomId, "owner", null);

        given(inviteLinkRepository.findById(1L)).willReturn(Optional.of(link));
        given(chatRoomMemberRepository.isOwner(roomId, userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> inviteLinkManager.deactivateLink(userId, 1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_FORBIDDEN);
    }
}
