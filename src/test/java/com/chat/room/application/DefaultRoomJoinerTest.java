package com.chat.room.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.group.domain.UserGroup;
import com.chat.group.infrastructure.UserGroupRepository;
import com.chat.profile.application.ProfileValidator;
import com.chat.profile.domain.Profile;
import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.domain.MemberRole;
import com.chat.room.domain.RoomGroupMembership;
import com.chat.room.dto.JoinPublicRoomRequest;
import com.chat.room.dto.RoomResponse;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import com.chat.room.infrastructure.RoomGroupMembershipRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultRoomJoinerTest {

    @InjectMocks
    private DefaultRoomJoiner roomJoiner;

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private ProfileValidator profileValidator;
    @Mock private RoomPasswordEncoder roomPasswordEncoder;
    @Mock private RoomCapacityPolicy roomCapacityPolicy;
    @Mock private UserGroupRepository userGroupRepository;
    @Mock private RoomGroupMembershipRepository roomGroupMembershipRepository;

    @Test
    @DisplayName("PUBLIC 방 비밀번호 없는 입장 성공")
    void joinPublicRoom_비밀번호없는_성공() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createPublic("owner", "공개방", null, "key");
        Profile profile = Profile.create(userId, "닉네임");
        UserGroup defaultGroup = UserGroup.createDefault(userId);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.countActiveByRoomId(roomId)).willReturn(3L);
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.empty());
        given(roomCapacityPolicy.maxCapacity(room.getType())).willReturn(10);
        given(profileValidator.validateOwnership(userId, 1L)).willReturn(profile);
        given(userGroupRepository.findDefaultByUserId(userId)).willReturn(Optional.of(defaultGroup));
        given(roomGroupMembershipRepository.findByRoomIdAndGroupId(any(), any())).willReturn(Optional.empty());
        given(chatRoomMemberRepository.save(any())).willReturn(ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER));

        // when
        RoomResponse response = roomJoiner.joinPublicRoom(userId, roomId, new JoinPublicRoomRequest(1L, null));

        // then
        assertThat(response).isNotNull();
        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));
        verify(roomGroupMembershipRepository).save(any(RoomGroupMembership.class));
    }

    @Test
    @DisplayName("PUBLIC 방 비밀번호 일치 입장 성공")
    void joinPublicRoom_비밀번호일치_성공() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createPublic("owner", "공개방", "hashed", "key");
        Profile profile = Profile.create(userId, "닉네임");
        UserGroup defaultGroup = UserGroup.createDefault(userId);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.countActiveByRoomId(roomId)).willReturn(3L);
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.empty());
        given(roomCapacityPolicy.maxCapacity(room.getType())).willReturn(10);
        given(profileValidator.validateOwnership(userId, 1L)).willReturn(profile);
        given(roomPasswordEncoder.matches("plain", "hashed")).willReturn(true);
        given(userGroupRepository.findDefaultByUserId(userId)).willReturn(Optional.of(defaultGroup));
        given(roomGroupMembershipRepository.findByRoomIdAndGroupId(any(), any())).willReturn(Optional.empty());
        given(chatRoomMemberRepository.save(any())).willReturn(ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER));

        // when
        RoomResponse response = roomJoiner.joinPublicRoom(userId, roomId, new JoinPublicRoomRequest(1L, "plain"));

        // then
        assertThat(response).isNotNull();
        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("존재하지 않는 방 입장 시 예외 발생")
    void joinPublicRoom_방없음_예외() {
        // given
        UUID roomId = UUID.randomUUID();
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomJoiner.joinPublicRoom("user-1", roomId, new JoinPublicRoomRequest(1L, null)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("PUBLIC이 아닌 방 입장 시 예외 발생")
    void joinPublicRoom_타입불일치_예외() {
        // given
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));

        // when & then
        assertThatThrownBy(() -> roomJoiner.joinPublicRoom("user-1", roomId, new JoinPublicRoomRequest(1L, null)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_TYPE_UNSUPPORTED);
    }

    @Test
    @DisplayName("빈 방 입장 시 예외 발생")
    void joinPublicRoom_빈방_예외() {
        // given
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createPublic("owner", "공개방", null, "key");
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.countActiveByRoomId(roomId)).willReturn(0L);

        // when & then
        assertThatThrownBy(() -> roomJoiner.joinPublicRoom("user-1", roomId, new JoinPublicRoomRequest(1L, null)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_EMPTY);
    }

    @Test
    @DisplayName("강퇴된 멤버 입장 시 예외 발생")
    void joinPublicRoom_강퇴멤버_예외() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createPublic("owner", "공개방", null, "key");
        ChatRoomMember kicked = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);
        kicked.kick();

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.countActiveByRoomId(roomId)).willReturn(3L);
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(kicked));

        // when & then
        assertThatThrownBy(() -> roomJoiner.joinPublicRoom(userId, roomId, new JoinPublicRoomRequest(1L, null)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_KICKED);
    }

    @Test
    @DisplayName("이미 입장한 멤버 재입장 시 예외 발생")
    void joinPublicRoom_이미입장_예외() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createPublic("owner", "공개방", null, "key");
        ChatRoomMember active = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.countActiveByRoomId(roomId)).willReturn(3L);
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(active));

        // when & then
        assertThatThrownBy(() -> roomJoiner.joinPublicRoom(userId, roomId, new JoinPublicRoomRequest(1L, null)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_ALREADY_JOINED);
    }

    @Test
    @DisplayName("인원 초과 시 예외 발생")
    void joinPublicRoom_인원초과_예외() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createPublic("owner", "공개방", null, "key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.countActiveByRoomId(roomId)).willReturn(10L);
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.empty());
        given(roomCapacityPolicy.maxCapacity(room.getType())).willReturn(10);

        // when & then
        assertThatThrownBy(() -> roomJoiner.joinPublicRoom(userId, roomId, new JoinPublicRoomRequest(1L, null)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_MEMBER_LIMIT);
    }

    @Test
    @DisplayName("비밀번호 불일치 시 예외 발생")
    void joinPublicRoom_비밀번호불일치_예외() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createPublic("owner", "공개방", "hashed", "key");
        Profile profile = Profile.create(userId, "닉네임");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.countActiveByRoomId(roomId)).willReturn(3L);
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.empty());
        given(roomCapacityPolicy.maxCapacity(room.getType())).willReturn(10);
        given(profileValidator.validateOwnership(userId, 1L)).willReturn(profile);
        given(roomPasswordEncoder.matches("wrong", "hashed")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> roomJoiner.joinPublicRoom(userId, roomId, new JoinPublicRoomRequest(1L, "wrong")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_PASSWORD_MISMATCH);
    }

    @Test
    @DisplayName("이전 퇴장 이력 있는 멤버 재입장 성공 — 기존 레코드 삭제 후 재생성")
    void joinPublicRoom_퇴장후재입장_성공() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createPublic("owner", "공개방", null, "key");
        ChatRoomMember left = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);
        left.leave();
        Profile profile = Profile.create(userId, "닉네임");
        UserGroup defaultGroup = UserGroup.createDefault(userId);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.countActiveByRoomId(roomId)).willReturn(3L);
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(left));
        given(roomCapacityPolicy.maxCapacity(room.getType())).willReturn(10);
        given(profileValidator.validateOwnership(userId, 1L)).willReturn(profile);
        given(userGroupRepository.findDefaultByUserId(userId)).willReturn(Optional.of(defaultGroup));
        given(roomGroupMembershipRepository.findByRoomIdAndGroupId(any(), any())).willReturn(Optional.empty());
        given(chatRoomMemberRepository.save(any())).willReturn(ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER));

        // when
        roomJoiner.joinPublicRoom(userId, roomId, new JoinPublicRoomRequest(1L, null));

        // then
        verify(chatRoomMemberRepository).delete(left);
        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("이미 기본 그룹에 방이 할당된 경우 중복 저장하지 않음")
    void joinPublicRoom_기본그룹이미할당_중복저장안함() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createPublic("owner", "공개방", null, "key");
        Profile profile = Profile.create(userId, "닉네임");
        UserGroup defaultGroup = UserGroup.createDefault(userId);
        RoomGroupMembership existing = RoomGroupMembership.create(roomId, 1L);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.countActiveByRoomId(roomId)).willReturn(3L);
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.empty());
        given(roomCapacityPolicy.maxCapacity(room.getType())).willReturn(10);
        given(profileValidator.validateOwnership(userId, 1L)).willReturn(profile);
        given(userGroupRepository.findDefaultByUserId(userId)).willReturn(Optional.of(defaultGroup));
        given(roomGroupMembershipRepository.findByRoomIdAndGroupId(any(), any())).willReturn(Optional.of(existing));
        given(chatRoomMemberRepository.save(any())).willReturn(ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER));

        // when
        roomJoiner.joinPublicRoom(userId, roomId, new JoinPublicRoomRequest(1L, null));

        // then
        verify(roomGroupMembershipRepository, never()).save(any(RoomGroupMembership.class));
    }
}
