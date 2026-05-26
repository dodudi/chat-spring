package com.chat.profile.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.profile.domain.Profile;
import com.chat.profile.dto.CreateProfileRequest;
import com.chat.profile.dto.ProfileResponse;
import com.chat.profile.dto.UpdateProfileRequest;
import com.chat.profile.infrastructure.ProfileRepository;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultProfileManagerTest {

    @InjectMocks
    private DefaultProfileManager profileManager;

    @Mock private ProfileRepository profileRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private ProfileEventPublisher profileEventPublisher;

    @Test
    @DisplayName("내 프로필 목록 조회 성공")
    void getMyProfiles_성공() {
        // given
        String userId = "user-1";
        Profile profile = Profile.create(userId, "닉네임");
        given(profileRepository.findAllByUserId(userId)).willReturn(List.of(profile));

        // when
        List<ProfileResponse> responses = profileManager.getMyProfiles(userId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).nickname()).isEqualTo("닉네임");
    }

    @Test
    @DisplayName("프로필 생성 성공")
    void createProfile_성공() {
        // given
        String userId = "user-1";
        Profile profile = Profile.create(userId, "새닉네임");
        given(profileRepository.save(any(Profile.class))).willReturn(profile);

        // when
        ProfileResponse response = profileManager.createProfile(userId, new CreateProfileRequest("새닉네임"));

        // then
        assertThat(response.nickname()).isEqualTo("새닉네임");
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    @DisplayName("닉네임 수정 성공")
    void updateNickname_성공() {
        // given
        String userId = "user-1";
        Profile profile = Profile.create(userId, "기존닉네임");
        given(profileRepository.findById(1L)).willReturn(Optional.of(profile));
        given(chatRoomMemberRepository.findActiveRoomIdsByProfileId(1L)).willReturn(List.of());

        // when
        ProfileResponse response = profileManager.updateNickname(userId, 1L, new UpdateProfileRequest("새닉네임"));

        // then
        assertThat(response.nickname()).isEqualTo("새닉네임");
    }

    @Test
    @DisplayName("닉네임 수정 성공 — 사용 중인 방이 있으면 afterCommit 발행 등록")
    void updateNickname_방있을때_afterCommit등록() {
        // given
        String userId = "user-1";
        Profile profile = Profile.create(userId, "기존닉네임");
        List<UUID> roomIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        given(profileRepository.findById(1L)).willReturn(Optional.of(profile));
        given(chatRoomMemberRepository.findActiveRoomIdsByProfileId(1L)).willReturn(roomIds);

        try (MockedStatic<TransactionSynchronizationManager> txManager =
                     mockStatic(TransactionSynchronizationManager.class)) {
            txManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                    .thenAnswer(inv -> null);

            // when
            profileManager.updateNickname(userId, 1L, new UpdateProfileRequest("새닉네임"));

            // then
            txManager.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()));
        }
    }

    @Test
    @DisplayName("닉네임 수정 시 사용 중인 방 없으면 발행 등록 안 함")
    void updateNickname_방없을때_afterCommit미등록() {
        // given
        String userId = "user-1";
        Profile profile = Profile.create(userId, "기존닉네임");
        given(profileRepository.findById(1L)).willReturn(Optional.of(profile));
        given(chatRoomMemberRepository.findActiveRoomIdsByProfileId(1L)).willReturn(List.of());

        try (MockedStatic<TransactionSynchronizationManager> txManager =
                     mockStatic(TransactionSynchronizationManager.class)) {
            // when
            profileManager.updateNickname(userId, 1L, new UpdateProfileRequest("새닉네임"));

            // then
            txManager.verify(
                    () -> TransactionSynchronizationManager.registerSynchronization(any()),
                    org.mockito.Mockito.never());
        }
    }

    @Test
    @DisplayName("존재하지 않는 프로필 수정 시 예외 발생")
    void updateNickname_프로필없음_예외() {
        // given
        given(profileRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> profileManager.updateNickname("user-1", 99L, new UpdateProfileRequest("새닉네임")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PROFILE_NOT_FOUND);
    }

    @Test
    @DisplayName("타인 프로필 수정 시 예외 발생")
    void updateNickname_소유권불일치_예외() {
        // given
        Profile profile = Profile.create("owner", "닉네임");
        given(profileRepository.findById(1L)).willReturn(Optional.of(profile));

        // when & then
        assertThatThrownBy(() -> profileManager.updateNickname("other-user", 1L, new UpdateProfileRequest("새닉네임")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PROFILE_FORBIDDEN);
    }

    @Test
    @DisplayName("프로필 삭제 성공")
    void deleteProfile_성공() {
        // given
        String userId = "user-1";
        Profile profile = Profile.create(userId, "닉네임");
        given(profileRepository.findById(1L)).willReturn(Optional.of(profile));
        given(chatRoomMemberRepository.existsByProfileId(1L)).willReturn(false);

        // when
        profileManager.deleteProfile(userId, 1L);

        // then
        verify(profileRepository).delete(profile);
    }

    @Test
    @DisplayName("사용 중인 프로필 삭제 시 예외 발생")
    void deleteProfile_사용중_예외() {
        // given
        String userId = "user-1";
        Profile profile = Profile.create(userId, "닉네임");
        given(profileRepository.findById(1L)).willReturn(Optional.of(profile));
        given(chatRoomMemberRepository.existsByProfileId(1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> profileManager.deleteProfile(userId, 1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PROFILE_IN_USE);

        verify(profileRepository, never()).delete(any());
    }

    @Test
    @DisplayName("소유권 검증 성공")
    void validateOwnership_성공() {
        // given
        String userId = "user-1";
        Profile profile = Profile.create(userId, "닉네임");
        given(profileRepository.findById(1L)).willReturn(Optional.of(profile));

        // when
        Profile result = profileManager.validateOwnership(userId, 1L);

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("소유권 검증 실패 — 타인 프로필 접근 시 예외 발생")
    void validateOwnership_소유권불일치_예외() {
        // given
        Profile profile = Profile.create("owner", "닉네임");
        given(profileRepository.findById(1L)).willReturn(Optional.of(profile));

        // when & then
        assertThatThrownBy(() -> profileManager.validateOwnership("other-user", 1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PROFILE_FORBIDDEN);
    }
}
