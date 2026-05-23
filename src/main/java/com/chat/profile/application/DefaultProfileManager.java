package com.chat.profile.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.profile.domain.Profile;
import com.chat.profile.dto.CreateProfileRequest;
import com.chat.profile.dto.ProfileResponse;
import com.chat.profile.dto.UpdateProfileRequest;
import com.chat.profile.infrastructure.ProfileRepository;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultProfileManager implements ProfileManager {

    private final ProfileRepository profileRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    public List<ProfileResponse> getMyProfiles(String userId) {
        return profileRepository.findAllByUserId(userId).stream()
                .map(ProfileResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public ProfileResponse createProfile(String userId, CreateProfileRequest request) {
        Profile profile = profileRepository.save(Profile.create(userId, request.nickname()));
        return ProfileResponse.from(profile);
    }

    @Override
    @Transactional
    public ProfileResponse updateNickname(String userId, Long profileId, UpdateProfileRequest request) {
        Profile profile = findOwnProfile(userId, profileId);
        profile.updateNickname(request.nickname());
        // TODO: PROFILE_UPDATED 브로드캐스트 — Redis 인프라 구성 후 추가
        return ProfileResponse.from(profile);
    }

    @Override
    @Transactional
    public void deleteProfile(String userId, Long profileId) {
        Profile profile = findOwnProfile(userId, profileId);
        if (chatRoomMemberRepository.existsByProfileId(profileId)) {
            throw new AppException(ErrorCode.PROFILE_IN_USE);
        }
        profileRepository.delete(profile);
    }

    private Profile findOwnProfile(String userId, Long profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));
        if (!profile.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.PROFILE_FORBIDDEN);
        }
        return profile;
    }
}
