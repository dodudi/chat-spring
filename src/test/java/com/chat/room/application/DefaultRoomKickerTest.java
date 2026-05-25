package com.chat.room.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DefaultRoomKickerTest {

    @InjectMocks
    private DefaultRoomKicker roomKicker;

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;

    @Test
    @DisplayName("방장이 멤버 강퇴 성공")
    void kickMember_success() {
        // given
        String requesterId = "owner";
        String targetUserId = "user-2";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup(requesterId, "그룹방", "key");
        ChatRoomMember target = ChatRoomMember.create(roomId, targetUserId, 2L, MemberRole.MEMBER);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, requesterId)).willReturn(true);
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, targetUserId)).willReturn(Optional.of(target));

        // when
        roomKicker.kickMember(requesterId, roomId, targetUserId);

        // then
        assertThat(target.getKickedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 방에서 강퇴 시 예외 발생")
    void kickMember_roomNotFound_throwsException() {
        // given
        UUID roomId = UUID.randomUUID();
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomKicker.kickMember("owner", roomId, "user-2"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("DM 방에서 강퇴 시 예외 발생")
    void kickMember_dmRoom_throwsException() {
        // given
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createDm("owner", "dm-key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));

        // when & then
        assertThatThrownBy(() -> roomKicker.kickMember("owner", roomId, "user-2"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_TYPE_UNSUPPORTED);
    }

    @Test
    @DisplayName("방장 아닌 사용자가 강퇴 시 예외 발생")
    void kickMember_notOwner_throwsException() {
        // given
        String requesterId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, requesterId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> roomKicker.kickMember(requesterId, roomId, "user-2"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_FORBIDDEN);
    }

    @Test
    @DisplayName("자기 자신을 강퇴 시 예외 발생")
    void kickMember_selfKick_throwsException() {
        // given
        String requesterId = "owner";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup(requesterId, "그룹방", "key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, requesterId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> roomKicker.kickMember(requesterId, roomId, requesterId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_SELF_KICK);
    }

    @Test
    @DisplayName("강퇴 대상 멤버가 없으면 예외 발생")
    void kickMember_targetNotFound_throwsException() {
        // given
        String requesterId = "owner";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup(requesterId, "그룹방", "key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, requesterId)).willReturn(true);
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, "user-2")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomKicker.kickMember(requesterId, roomId, "user-2"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 강퇴된 멤버를 재강퇴 시 예외 발생")
    void kickMember_alreadyKicked_throwsException() {
        // given
        String requesterId = "owner";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup(requesterId, "그룹방", "key");
        ChatRoomMember target = ChatRoomMember.create(roomId, "user-2", 2L, MemberRole.MEMBER);
        target.kick();

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, requesterId)).willReturn(true);
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, "user-2")).willReturn(Optional.of(target));

        // when & then
        assertThatThrownBy(() -> roomKicker.kickMember(requesterId, roomId, "user-2"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_MEMBER_NOT_FOUND);
    }
}
