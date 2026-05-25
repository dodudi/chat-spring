package com.chat.room.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.domain.MemberRole;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import com.chat.room.infrastructure.RoomGroupMembershipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultRoomLeaverTest {

    @InjectMocks
    private DefaultRoomLeaver roomLeaver;

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private RoomGroupMembershipRepository roomGroupMembershipRepository;

    @Test
    @DisplayName("DM 방 나가기 — 숨김 처리 성공")
    void leaveRoom_dmRoom_hidesSuccessfully() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createDm(userId, "dm-key");
        ChatRoomMember member = ChatRoomMember.create(roomId, userId, 1L, MemberRole.OWNER);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));

        // when
        roomLeaver.leaveRoom(userId, roomId);

        // then
        assertThat(member.isHidden()).isTrue();
        assertThat(member.getHiddenAt()).isNotNull();
    }

    @Test
    @DisplayName("DM 방 이미 숨김 처리된 경우 멱등 — hiddenAt 변경 없음")
    void leaveRoom_dmRoom_alreadyHidden_noOp() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createDm(userId, "dm-key");
        ChatRoomMember member = ChatRoomMember.create(roomId, userId, 1L, MemberRole.OWNER);
        member.hide();
        var hiddenAt = member.getHiddenAt();

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));

        // when
        roomLeaver.leaveRoom(userId, roomId);

        // then
        assertThat(member.getHiddenAt()).isEqualTo(hiddenAt);
    }

    @Test
    @DisplayName("GROUP 방 일반 멤버 나가기 — leftAt 기록")
    void leaveRoom_groupRoom_member_leavesSuccessfully() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");
        ChatRoomMember member = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));

        // when
        roomLeaver.leaveRoom(userId, roomId);

        // then
        assertThat(member.getLeftAt()).isNotNull();
        verify(roomGroupMembershipRepository).deleteByRoomIdAndUserId(roomId, userId);
    }

    @Test
    @DisplayName("GROUP 방 방장 나가기 — 다음 멤버에게 OWNER 위임")
    void leaveRoom_groupRoom_owner_delegatesToNextMember() {
        // given
        String ownerId = "owner";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup(ownerId, "그룹방", "key");
        ChatRoomMember owner = ChatRoomMember.create(roomId, ownerId, 1L, MemberRole.OWNER);
        ChatRoomMember nextMember = ChatRoomMember.create(roomId, "user-2", 2L, MemberRole.MEMBER);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, ownerId)).willReturn(Optional.of(owner));
        given(chatRoomMemberRepository.findActiveMembersExcluding(any(), any(), any())).willReturn(List.of(nextMember));

        // when
        roomLeaver.leaveRoom(ownerId, roomId);

        // then
        assertThat(nextMember.getRole()).isEqualTo(MemberRole.OWNER);
        assertThat(owner.getLeftAt()).isNotNull();
    }

    @Test
    @DisplayName("GROUP 방 방장이 마지막 멤버일 때 — 위임 없이 나가기")
    void leaveRoom_groupRoom_lastMember_leavesWithoutDelegation() {
        // given
        String ownerId = "owner";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup(ownerId, "그룹방", "key");
        ChatRoomMember owner = ChatRoomMember.create(roomId, ownerId, 1L, MemberRole.OWNER);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, ownerId)).willReturn(Optional.of(owner));
        given(chatRoomMemberRepository.findActiveMembersExcluding(any(), any(), any())).willReturn(List.of());

        // when
        roomLeaver.leaveRoom(ownerId, roomId);

        // then
        assertThat(owner.getLeftAt()).isNotNull();
        assertThat(owner.getRole()).isEqualTo(MemberRole.MEMBER);
    }

    @Test
    @DisplayName("PUBLIC 방 나가기 — 멤버 레코드 삭제")
    void leaveRoom_publicRoom_deletesMember() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createPublic("owner", "공개방", "hashed", "key");
        ChatRoomMember member = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));

        // when
        roomLeaver.leaveRoom(userId, roomId);

        // then
        verify(chatRoomMemberRepository).delete(member);
        verify(roomGroupMembershipRepository).deleteByRoomIdAndUserId(roomId, userId);
    }

    @Test
    @DisplayName("존재하지 않는 방 나가기 시 예외 발생")
    void leaveRoom_roomNotFound_throwsException() {
        // given
        UUID roomId = UUID.randomUUID();
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomLeaver.leaveRoom("user-1", roomId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 퇴장한 멤버가 GROUP 방 나가기 시 예외 발생")
    void leaveRoom_groupRoom_alreadyLeft_throwsException() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");
        ChatRoomMember member = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);
        member.leave();

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> roomLeaver.leaveRoom(userId, roomId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_MEMBER_NOT_FOUND);
    }
}
