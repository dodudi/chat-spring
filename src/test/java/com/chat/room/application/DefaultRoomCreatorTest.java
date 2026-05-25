package com.chat.room.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.group.domain.UserGroup;
import com.chat.group.infrastructure.UserGroupRepository;
import com.chat.profile.application.ProfileValidator;
import com.chat.profile.domain.Profile;
import com.chat.profile.infrastructure.ProfileRepository;
import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.domain.RoomGroupMembership;
import com.chat.room.dto.CreateDmRoomRequest;
import com.chat.room.dto.CreateGroupRoomRequest;
import com.chat.room.dto.DmRoomResponse;
import com.chat.room.dto.RoomResponse;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import com.chat.room.infrastructure.RoomGroupMembershipRepository;
import com.chat.user.infrastructure.UserRepository;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultRoomCreatorTest {

    @InjectMocks
    private DefaultRoomCreator roomCreator;

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private RoomGroupMembershipRepository roomGroupMembershipRepository;
    @Mock private ProfileValidator profileValidator;
    @Mock private ProfileRepository profileRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserGroupRepository userGroupRepository;
    @Mock private RoomKeyCreator roomKeyCreator;
    @Mock private RoomPasswordEncoder roomPasswordEncoder;

    @Test
    @DisplayName("그룹 채팅방 생성 성공")
    void createGroupRoom_success() {
        // given
        String userId = "user-1";
        CreateGroupRoomRequest request = new CreateGroupRoomRequest("테스트방", 1L);

        Profile profile = Profile.create(userId, "닉네임");
        ChatRoom room = ChatRoom.createGroup(userId, request.name(), "group-key-1");
        UserGroup defaultGroup = UserGroup.createDefault(userId);

        given(profileValidator.validateOwnership(userId, 1L)).willReturn(profile);
        given(roomKeyCreator.createGroupRoomKey()).willReturn("group-key-1");
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(room);
        given(chatRoomMemberRepository.save(any(ChatRoomMember.class))).willReturn(null);
        given(userGroupRepository.findDefaultByUserId(userId)).willReturn(Optional.of(defaultGroup));
        given(roomGroupMembershipRepository.save(any(RoomGroupMembership.class))).willReturn(null);

        // when
        RoomResponse response = roomCreator.createGroupRoom(userId, request);

        // then
        assertThat(response.name()).isEqualTo("테스트방");
        assertThat(response.type()).isEqualTo("GROUP");
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("그룹 채팅방 생성 시 기본 그룹이 없으면 예외 발생")
    void createGroupRoom_defaultGroupMissing_throwsException() {
        // given
        String userId = "user-1";
        CreateGroupRoomRequest request = new CreateGroupRoomRequest("테스트방", 1L);

        given(profileValidator.validateOwnership(userId, 1L)).willReturn(Profile.create(userId, "닉네임"));
        given(roomKeyCreator.createGroupRoomKey()).willReturn("group-key-1");
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(ChatRoom.createGroup(userId, request.name(), "group-key-1"));
        given(userGroupRepository.findDefaultByUserId(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomCreator.createGroupRoom(userId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.GROUP_NOT_FOUND);
    }

    @Test
    @DisplayName("DM 방 생성 시 대상 유저가 없으면 예외 발생")
    void createDmRoom_targetUserNotFound_throwsException() {
        // given
        String userId = "user-1";
        CreateDmRoomRequest request = new CreateDmRoomRequest("user-2", 1L);

        given(userRepository.existsById("user-2")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> roomCreator.createDmRoom(userId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("DM 방이 이미 존재하면 기존 방 반환")
    void createDmRoom_existingRoom_returnsExisting() {
        // given
        String userId = "user-1";
        String targetUserId = "user-2";
        CreateDmRoomRequest request = new CreateDmRoomRequest(targetUserId, 1L);

        Profile profile = Profile.create(userId, "닉네임");
        ChatRoom existing = ChatRoom.createDm(userId, "dm-key");

        given(userRepository.existsById(targetUserId)).willReturn(true);
        given(profileValidator.validateOwnership(userId, 1L)).willReturn(profile);
        given(roomKeyCreator.createDmRoomKey(userId, targetUserId)).willReturn("dm-key");
        given(chatRoomRepository.findByRoomKey("dm-key")).willReturn(Optional.of(existing));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(existing.getId(), userId)).willReturn(Optional.empty());

        // when
        DmRoomResponse response = roomCreator.createDmRoom(userId, request);

        // then
        assertThat(response.type()).isEqualTo("DM");
        assertThat(response.roomKey()).isEqualTo("dm-key");
    }

    @Test
    @DisplayName("DM 방이 없으면 새로 생성")
    void createDmRoom_noExistingRoom_createsNew() {
        // given
        String userId = "user-1";
        String targetUserId = "user-2";
        CreateDmRoomRequest request = new CreateDmRoomRequest(targetUserId, 1L);

        Profile creatorProfile = Profile.create(userId, "닉네임");
        Profile targetProfile = Profile.create(targetUserId, "상대닉네임");
        ChatRoom newRoom = ChatRoom.createDm(userId, "dm-key");
        UserGroup defaultGroup = UserGroup.createDefault(userId);

        given(userRepository.existsById(targetUserId)).willReturn(true);
        given(profileValidator.validateOwnership(userId, 1L)).willReturn(creatorProfile);
        given(roomKeyCreator.createDmRoomKey(userId, targetUserId)).willReturn("dm-key");
        given(chatRoomRepository.findByRoomKey("dm-key")).willReturn(Optional.empty());
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(newRoom);
        given(profileRepository.findFirstByUserId(targetUserId)).willReturn(Optional.of(targetProfile));
        given(userGroupRepository.findDefaultByUserId(any())).willReturn(Optional.of(defaultGroup));

        // when
        DmRoomResponse response = roomCreator.createDmRoom(userId, request);

        // then
        assertThat(response.type()).isEqualTo("DM");
        assertThat(response.roomKey()).isEqualTo("dm-key");
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(chatRoomMemberRepository, times(2)).save(any(ChatRoomMember.class));
    }
}
