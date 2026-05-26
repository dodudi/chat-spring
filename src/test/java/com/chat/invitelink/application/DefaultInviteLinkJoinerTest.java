package com.chat.invitelink.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.group.domain.UserGroup;
import com.chat.group.infrastructure.UserGroupRepository;
import com.chat.invitelink.domain.InviteLink;
import com.chat.invitelink.dto.JoinByLinkRequest;
import com.chat.invitelink.infrastructure.InviteLinkRepository;
import com.chat.profile.application.ProfileValidator;
import com.chat.profile.domain.Profile;
import com.chat.room.application.RoomCapacityPolicy;
import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.domain.MemberRole;
import com.chat.room.domain.RoomGroupMembership;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import com.chat.room.infrastructure.RoomGroupMembershipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultInviteLinkJoinerTest {

    @InjectMocks
    private DefaultInviteLinkJoiner inviteLinkJoiner;

    @Mock private InviteLinkRepository inviteLinkRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private ProfileValidator profileValidator;
    @Mock private RoomCapacityPolicy roomCapacityPolicy;
    @Mock private UserGroupRepository userGroupRepository;
    @Mock private RoomGroupMembershipRepository roomGroupMembershipRepository;

    private static final JoinByLinkRequest REQUEST = new JoinByLinkRequest(1L);

    @Test
    @DisplayName("초대 링크로 방 입장 성공")
    void joinByLink_성공() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        InviteLink link = InviteLink.create(roomId, "owner", null);
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");
        Profile profile = Profile.create(userId, "닉네임");
        UserGroup defaultGroup = UserGroup.createDefault(userId);

        given(inviteLinkRepository.findByToken("token123")).willReturn(Optional.of(link));
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.countActiveByRoomId(any())).willReturn(3L);
        given(roomCapacityPolicy.maxCapacity(room.getType())).willReturn(10);
        given(chatRoomMemberRepository.findByRoomIdAndUserId(any(), anyString())).willReturn(Optional.empty());
        given(profileValidator.validateOwnership(userId, 1L)).willReturn(profile);
        given(chatRoomMemberRepository.save(any())).willReturn(ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER));
        given(userGroupRepository.findDefaultByUserId(userId)).willReturn(Optional.of(defaultGroup));
        given(roomGroupMembershipRepository.findByRoomIdAndGroupId(any(), any())).willReturn(Optional.empty());

        // when
        inviteLinkJoiner.joinByLink(userId, "token123", REQUEST);

        // then
        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));
        verify(roomGroupMembershipRepository).save(any(RoomGroupMembership.class));
    }

    @Test
    @DisplayName("존재하지 않는 토큰 사용 시 예외 발생")
    void joinByLink_토큰없음_예외() {
        // given
        given(inviteLinkRepository.findByToken("invalid")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inviteLinkJoiner.joinByLink("user-1", "invalid", REQUEST))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("비활성화된 링크 사용 시 예외 발생")
    void joinByLink_비활성링크_예외() {
        // given
        UUID roomId = UUID.randomUUID();
        InviteLink link = InviteLink.create(roomId, "owner", null);
        link.deactivate();
        given(inviteLinkRepository.findByToken("token123")).willReturn(Optional.of(link));

        // when & then
        assertThatThrownBy(() -> inviteLinkJoiner.joinByLink("user-1", "token123", REQUEST))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVITE_LINK_INACTIVE);
    }

    @Test
    @DisplayName("만료된 링크 사용 시 예외 발생")
    void joinByLink_만료링크_예외() {
        // given
        UUID roomId = UUID.randomUUID();
        InviteLink link = InviteLink.create(roomId, "owner", OffsetDateTime.now().minusSeconds(1));
        given(inviteLinkRepository.findByToken("token123")).willReturn(Optional.of(link));

        // when & then
        assertThatThrownBy(() -> inviteLinkJoiner.joinByLink("user-1", "token123", REQUEST))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVITE_LINK_EXPIRED);
    }

    @Test
    @DisplayName("인원 초과 시 예외 발생")
    void joinByLink_인원초과_예외() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        InviteLink link = InviteLink.create(roomId, "owner", null);
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");

        given(inviteLinkRepository.findByToken("token123")).willReturn(Optional.of(link));
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.countActiveByRoomId(any())).willReturn(10L);
        given(roomCapacityPolicy.maxCapacity(room.getType())).willReturn(10);

        // when & then
        assertThatThrownBy(() -> inviteLinkJoiner.joinByLink(userId, "token123", REQUEST))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_MEMBER_LIMIT);
    }

    @Test
    @DisplayName("이미 입장한 멤버가 링크로 재입장 시 예외 발생")
    void joinByLink_이미입장_예외() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        InviteLink link = InviteLink.create(roomId, "owner", null);
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");
        ChatRoomMember active = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);

        given(inviteLinkRepository.findByToken("token123")).willReturn(Optional.of(link));
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.countActiveByRoomId(any())).willReturn(3L);
        given(roomCapacityPolicy.maxCapacity(room.getType())).willReturn(10);
        given(chatRoomMemberRepository.findByRoomIdAndUserId(any(), anyString())).willReturn(Optional.of(active));

        // when & then
        assertThatThrownBy(() -> inviteLinkJoiner.joinByLink(userId, "token123", REQUEST))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_ALREADY_JOINED);
    }

    @Test
    @DisplayName("강퇴된 멤버도 링크로 재입장 성공 — 기존 레코드 삭제 후 재생성")
    void joinByLink_강퇴멤버_재입장_성공() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        InviteLink link = InviteLink.create(roomId, "owner", null);
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");
        ChatRoomMember kicked = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);
        kicked.kick();
        Profile profile = Profile.create(userId, "닉네임");
        UserGroup defaultGroup = UserGroup.createDefault(userId);

        given(inviteLinkRepository.findByToken("token123")).willReturn(Optional.of(link));
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.countActiveByRoomId(any())).willReturn(3L);
        given(roomCapacityPolicy.maxCapacity(room.getType())).willReturn(10);
        given(chatRoomMemberRepository.findByRoomIdAndUserId(any(), anyString())).willReturn(Optional.of(kicked));
        given(profileValidator.validateOwnership(userId, 1L)).willReturn(profile);
        given(chatRoomMemberRepository.save(any())).willReturn(ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER));
        given(userGroupRepository.findDefaultByUserId(userId)).willReturn(Optional.of(defaultGroup));
        given(roomGroupMembershipRepository.findByRoomIdAndGroupId(any(), any())).willReturn(Optional.empty());

        // when
        inviteLinkJoiner.joinByLink(userId, "token123", REQUEST);

        // then
        verify(chatRoomMemberRepository).delete(kicked);
        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));
    }
}
