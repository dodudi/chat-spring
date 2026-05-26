package com.chat.group.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.group.domain.UserGroup;
import com.chat.group.dto.CreateGroupRequest;
import com.chat.group.dto.GroupResponse;
import com.chat.group.dto.UpdateGroupRequest;
import com.chat.group.infrastructure.UserGroupRepository;
import com.chat.room.infrastructure.RoomGroupMembershipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultGroupManagerTest {

    @InjectMocks
    private DefaultGroupManager groupManager;

    @Mock private UserGroupRepository userGroupRepository;
    @Mock private RoomGroupMembershipRepository roomGroupMembershipRepository;
    @Mock private GroupPolicy groupPolicy;

    @Test
    @DisplayName("그룹 생성 성공")
    void createGroup_성공() {
        // given
        String userId = "user-1";
        UserGroup group = UserGroup.create(userId, "스터디");
        given(userGroupRepository.existsByUserIdAndName(userId, "스터디")).willReturn(false);
        given(groupPolicy.maxGroupCount()).willReturn(10);
        given(userGroupRepository.countCustomByUserId(userId)).willReturn(3L);
        given(userGroupRepository.save(any(UserGroup.class))).willReturn(group);

        // when
        GroupResponse response = groupManager.createGroup(userId, new CreateGroupRequest("스터디"));

        // then
        assertThat(response.name()).isEqualTo("스터디");
        verify(userGroupRepository).save(any(UserGroup.class));
    }

    @Test
    @DisplayName("그룹 이름 중복 시 예외 발생")
    void createGroup_이름중복_예외() {
        // given
        String userId = "user-1";
        given(userGroupRepository.existsByUserIdAndName(userId, "스터디")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> groupManager.createGroup(userId, new CreateGroupRequest("스터디")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.GROUP_NAME_DUPLICATE);
    }

    @Test
    @DisplayName("그룹 개수 한도 초과 시 예외 발생")
    void createGroup_한도초과_예외() {
        // given
        String userId = "user-1";
        given(userGroupRepository.existsByUserIdAndName(userId, "스터디")).willReturn(false);
        given(groupPolicy.maxGroupCount()).willReturn(10);
        given(userGroupRepository.countCustomByUserId(userId)).willReturn(10L);

        // when & then
        assertThatThrownBy(() -> groupManager.createGroup(userId, new CreateGroupRequest("스터디")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.GROUP_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("그룹 이름 변경 성공")
    void renameGroup_성공() {
        // given
        String userId = "user-1";
        UserGroup group = UserGroup.create(userId, "기존이름");
        given(userGroupRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(group));
        given(userGroupRepository.existsByUserIdAndNameExcluding(userId, "새이름", 1L)).willReturn(false);

        // when
        GroupResponse response = groupManager.renameGroup(userId, 1L, new UpdateGroupRequest("새이름"));

        // then
        assertThat(response.name()).isEqualTo("새이름");
    }

    @Test
    @DisplayName("기본 그룹 이름 변경 시 예외 발생")
    void renameGroup_기본그룹_예외() {
        // given
        String userId = "user-1";
        UserGroup defaultGroup = UserGroup.createDefault(userId);
        given(userGroupRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(defaultGroup));

        // when & then
        assertThatThrownBy(() -> groupManager.renameGroup(userId, 1L, new UpdateGroupRequest("새이름")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.GROUP_DEFAULT_IMMUTABLE);
    }

    @Test
    @DisplayName("이름 변경 시 이름 중복 예외 발생")
    void renameGroup_이름중복_예외() {
        // given
        String userId = "user-1";
        UserGroup group = UserGroup.create(userId, "기존이름");
        given(userGroupRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(group));
        given(userGroupRepository.existsByUserIdAndNameExcluding(userId, "새이름", 1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> groupManager.renameGroup(userId, 1L, new UpdateGroupRequest("새이름")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.GROUP_NAME_DUPLICATE);
    }

    @Test
    @DisplayName("존재하지 않는 그룹 이름 변경 시 예외 발생")
    void renameGroup_그룹없음_예외() {
        // given
        String userId = "user-1";
        given(userGroupRepository.findByIdAndUserId(99L, userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> groupManager.renameGroup(userId, 99L, new UpdateGroupRequest("새이름")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.GROUP_NOT_FOUND);
    }

    @Test
    @DisplayName("그룹 삭제 성공 — 방 연결도 함께 삭제")
    void deleteGroup_성공() {
        // given
        String userId = "user-1";
        UserGroup group = UserGroup.create(userId, "스터디");
        given(userGroupRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(group));

        // when
        groupManager.deleteGroup(userId, 1L);

        // then
        verify(roomGroupMembershipRepository).deleteByGroupId(group.getId());
        verify(userGroupRepository).delete(group);
    }

    @Test
    @DisplayName("기본 그룹 삭제 시 예외 발생")
    void deleteGroup_기본그룹_예외() {
        // given
        String userId = "user-1";
        UserGroup defaultGroup = UserGroup.createDefault(userId);
        given(userGroupRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(defaultGroup));

        // when & then
        assertThatThrownBy(() -> groupManager.deleteGroup(userId, 1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.GROUP_DEFAULT_IMMUTABLE);
    }

    @Test
    @DisplayName("존재하지 않는 그룹 삭제 시 예외 발생")
    void deleteGroup_그룹없음_예외() {
        // given
        String userId = "user-1";
        given(userGroupRepository.findByIdAndUserId(99L, userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> groupManager.deleteGroup(userId, 99L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.GROUP_NOT_FOUND);
    }
}
