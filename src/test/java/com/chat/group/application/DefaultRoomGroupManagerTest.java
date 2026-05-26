package com.chat.group.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.group.domain.UserGroup;
import com.chat.group.infrastructure.UserGroupRepository;
import com.chat.room.domain.RoomGroupMembership;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.RoomGroupMembershipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultRoomGroupManagerTest {

    @InjectMocks
    private DefaultRoomGroupManager roomGroupManager;

    @Mock private UserGroupRepository userGroupRepository;
    @Mock private RoomGroupMembershipRepository roomGroupMembershipRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;

    @Test
    @DisplayName("방을 그룹에 할당 성공")
    void assignRoom_성공() {
        // given
        String userId = "user-1";
        Long groupId = 1L;
        UUID roomId = UUID.randomUUID();
        UserGroup group = UserGroup.create(userId, "스터디");

        given(userGroupRepository.findByIdAndUserId(groupId, userId)).willReturn(Optional.of(group));
        given(chatRoomMemberRepository.existsActiveMember(roomId, userId)).willReturn(true);
        given(roomGroupMembershipRepository.findByRoomIdAndGroupId(roomId, group.getId())).willReturn(Optional.empty());

        // when
        roomGroupManager.assignRoom(userId, groupId, roomId);

        // then
        verify(roomGroupMembershipRepository).save(any(RoomGroupMembership.class));
    }

    @Test
    @DisplayName("그룹 없을 시 예외 발생")
    void assignRoom_그룹없음_예외() {
        // given
        String userId = "user-1";
        given(userGroupRepository.findByIdAndUserId(99L, userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomGroupManager.assignRoom(userId, 99L, UUID.randomUUID()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.GROUP_NOT_FOUND);
    }

    @Test
    @DisplayName("방 멤버가 아닌 경우 할당 시 예외 발생")
    void assignRoom_멤버아님_예외() {
        // given
        String userId = "user-1";
        Long groupId = 1L;
        UUID roomId = UUID.randomUUID();
        UserGroup group = UserGroup.create(userId, "스터디");

        given(userGroupRepository.findByIdAndUserId(groupId, userId)).willReturn(Optional.of(group));
        given(chatRoomMemberRepository.existsActiveMember(roomId, userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> roomGroupManager.assignRoom(userId, groupId, roomId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("이미 할당된 방 재할당 시 예외 발생")
    void assignRoom_이미할당_예외() {
        // given
        String userId = "user-1";
        Long groupId = 1L;
        UUID roomId = UUID.randomUUID();
        UserGroup group = UserGroup.create(userId, "스터디");
        RoomGroupMembership existing = RoomGroupMembership.create(roomId, group.getId());

        given(userGroupRepository.findByIdAndUserId(groupId, userId)).willReturn(Optional.of(group));
        given(chatRoomMemberRepository.existsActiveMember(roomId, userId)).willReturn(true);
        given(roomGroupMembershipRepository.findByRoomIdAndGroupId(roomId, group.getId())).willReturn(Optional.of(existing));

        // when & then
        assertThatThrownBy(() -> roomGroupManager.assignRoom(userId, groupId, roomId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.GROUP_ROOM_ALREADY_ASSIGNED);
    }

    @Test
    @DisplayName("방을 그룹에서 제거 성공")
    void removeRoom_성공() {
        // given
        String userId = "user-1";
        Long groupId = 1L;
        UUID roomId = UUID.randomUUID();
        UserGroup group = UserGroup.create(userId, "스터디");

        given(userGroupRepository.findByIdAndUserId(groupId, userId)).willReturn(Optional.of(group));

        // when
        roomGroupManager.removeRoom(userId, groupId, roomId);

        // then
        verify(roomGroupMembershipRepository).deleteByRoomIdAndGroupId(roomId, group.getId());
    }

    @Test
    @DisplayName("기본 그룹에서 방 제거 시 예외 발생")
    void removeRoom_기본그룹_예외() {
        // given
        String userId = "user-1";
        Long groupId = 1L;
        UserGroup defaultGroup = UserGroup.createDefault(userId);

        given(userGroupRepository.findByIdAndUserId(groupId, userId)).willReturn(Optional.of(defaultGroup));

        // when & then
        assertThatThrownBy(() -> roomGroupManager.removeRoom(userId, groupId, UUID.randomUUID()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.GROUP_DEFAULT_IMMUTABLE);
    }

    @Test
    @DisplayName("존재하지 않는 그룹에서 방 제거 시 예외 발생")
    void removeRoom_그룹없음_예외() {
        // given
        String userId = "user-1";
        given(userGroupRepository.findByIdAndUserId(99L, userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomGroupManager.removeRoom(userId, 99L, UUID.randomUUID()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.GROUP_NOT_FOUND);
    }
}
