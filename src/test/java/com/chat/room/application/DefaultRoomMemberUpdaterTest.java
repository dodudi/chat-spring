package com.chat.room.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.profile.application.ProfileValidator;
import com.chat.profile.domain.Profile;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.domain.MemberRole;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
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
class DefaultRoomMemberUpdaterTest {

    @InjectMocks
    private DefaultRoomMemberUpdater roomMemberUpdater;

    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private ProfileValidator profileValidator;

    @Test
    @DisplayName("채팅방 프로필 변경 성공")
    void updateProfile_성공() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoomMember member = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);
        Profile newProfile = Profile.create(userId, "새닉네임");

        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));
        given(profileValidator.validateOwnership(userId, 2L)).willReturn(newProfile);

        // when
        roomMemberUpdater.updateProfile(userId, roomId, 2L);

        // then
        assertThat(member.getProfileId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("비활성 멤버의 프로필 변경 시 예외 발생")
    void updateProfile_비활성멤버_예외() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoomMember left = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);
        left.leave();

        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(left));

        // when & then
        assertThatThrownBy(() -> roomMemberUpdater.updateProfile(userId, roomId, 2L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 멤버의 프로필 변경 시 예외 발생")
    void updateProfile_멤버없음_예외() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomMemberUpdater.updateProfile(userId, roomId, 2L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("소유하지 않은 프로필로 변경 시 예외 발생")
    void updateProfile_소유권불일치_예외() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoomMember member = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);

        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));
        given(profileValidator.validateOwnership(userId, 2L))
                .willThrow(new AppException(ErrorCode.PROFILE_FORBIDDEN));

        // when & then
        assertThatThrownBy(() -> roomMemberUpdater.updateProfile(userId, roomId, 2L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PROFILE_FORBIDDEN);
    }
}
