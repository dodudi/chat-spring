package com.chat.invitation.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.group.domain.UserGroup;
import com.chat.group.infrastructure.UserGroupRepository;
import com.chat.invitation.domain.Invitation;
import com.chat.invitation.domain.InvitationStatus;
import com.chat.invitation.dto.AcceptInvitationRequest;
import com.chat.invitation.infrastructure.InvitationRepository;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultInvitationResponderTest {

    @InjectMocks
    private DefaultInvitationResponder invitationResponder;

    @Mock private InvitationRepository invitationRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private ProfileValidator profileValidator;
    @Mock private RoomCapacityPolicy roomCapacityPolicy;
    @Mock private UserGroupRepository userGroupRepository;
    @Mock private RoomGroupMembershipRepository roomGroupMembershipRepository;

    private static final AcceptInvitationRequest ACCEPT_REQUEST = new AcceptInvitationRequest(1L);

    @Test
    @DisplayName("초대 수락 성공 — 멤버 추가 및 기본 그룹 할당")
    void accept_성공() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        Invitation invitation = Invitation.create(roomId, "owner", userId);
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");
        Profile profile = Profile.create(userId, "닉네임");
        UserGroup defaultGroup = UserGroup.createDefault(userId);

        given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.countActiveByRoomId(any())).willReturn(3L);
        given(roomCapacityPolicy.maxCapacity(room.getType())).willReturn(10);
        given(chatRoomMemberRepository.findByRoomIdAndUserId(any(), anyString())).willReturn(Optional.empty());
        given(profileValidator.validateOwnership(userId, 1L)).willReturn(profile);
        given(chatRoomMemberRepository.save(any())).willReturn(ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER));
        given(userGroupRepository.findDefaultByUserId(userId)).willReturn(Optional.of(defaultGroup));
        given(roomGroupMembershipRepository.findByRoomIdAndGroupId(any(), any())).willReturn(Optional.empty());

        // when
        invitationResponder.accept(userId, 1L, ACCEPT_REQUEST);

        // then
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));
        verify(roomGroupMembershipRepository).save(any(RoomGroupMembership.class));
    }

    @Test
    @DisplayName("초대 수락 시 인원 초과 예외 발생")
    void accept_인원초과_예외() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        Invitation invitation = Invitation.create(roomId, "owner", userId);
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");

        given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.countActiveByRoomId(any())).willReturn(10L);
        given(roomCapacityPolicy.maxCapacity(room.getType())).willReturn(10);

        // when & then
        assertThatThrownBy(() -> invitationResponder.accept(userId, 1L, ACCEPT_REQUEST))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_MEMBER_LIMIT);
    }

    @Test
    @DisplayName("초대 수락 시 강퇴된 멤버인 경우 예외 발생")
    void accept_강퇴멤버_예외() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        Invitation invitation = Invitation.create(roomId, "owner", userId);
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");
        ChatRoomMember kicked = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);
        kicked.kick();

        given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.countActiveByRoomId(any())).willReturn(3L);
        given(roomCapacityPolicy.maxCapacity(room.getType())).willReturn(10);
        given(chatRoomMemberRepository.findByRoomIdAndUserId(any(), anyString())).willReturn(Optional.of(kicked));

        // when & then
        assertThatThrownBy(() -> invitationResponder.accept(userId, 1L, ACCEPT_REQUEST))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_KICKED);
    }

    @Test
    @DisplayName("초대 수락 시 이미 활성 멤버인 경우 예외 발생")
    void accept_이미멤버_예외() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        Invitation invitation = Invitation.create(roomId, "owner", userId);
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");
        ChatRoomMember active = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);

        given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.countActiveByRoomId(any())).willReturn(3L);
        given(roomCapacityPolicy.maxCapacity(room.getType())).willReturn(10);
        given(chatRoomMemberRepository.findByRoomIdAndUserId(any(), anyString())).willReturn(Optional.of(active));

        // when & then
        assertThatThrownBy(() -> invitationResponder.accept(userId, 1L, ACCEPT_REQUEST))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVITATION_ALREADY_MEMBER);
    }

    @Test
    @DisplayName("초대 없을 시 수락 예외 발생")
    void accept_초대없음_예외() {
        // given
        given(invitationRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> invitationResponder.accept("user-1", 1L, ACCEPT_REQUEST))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVITATION_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 초대가 아닌 경우 수락 예외 발생")
    void accept_타인초대_예외() {
        // given
        UUID roomId = UUID.randomUUID();
        Invitation invitation = Invitation.create(roomId, "owner", "other-user");
        given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));

        // when & then
        assertThatThrownBy(() -> invitationResponder.accept("user-1", 1L, ACCEPT_REQUEST))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("이미 처리된 초대 수락 시 예외 발생")
    void accept_이미처리된초대_예외() {
        // given
        UUID roomId = UUID.randomUUID();
        Invitation invitation = Invitation.create(roomId, "owner", "user-1");
        invitation.accept();
        given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));

        // when & then
        assertThatThrownBy(() -> invitationResponder.accept("user-1", 1L, ACCEPT_REQUEST))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVITATION_ALREADY_PROCESSED);
    }

    @Test
    @DisplayName("초대 거절 성공")
    void reject_성공() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        Invitation invitation = Invitation.create(roomId, "owner", userId);
        given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));

        // when
        invitationResponder.reject(userId, 1L);

        // then
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.REJECTED);
    }

    @Test
    @DisplayName("이미 처리된 초대 거절 시 예외 발생")
    void reject_이미처리된초대_예외() {
        // given
        UUID roomId = UUID.randomUUID();
        Invitation invitation = Invitation.create(roomId, "owner", "user-1");
        invitation.reject();
        given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));

        // when & then
        assertThatThrownBy(() -> invitationResponder.reject("user-1", 1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVITATION_ALREADY_PROCESSED);
    }
}
