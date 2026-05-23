package com.chat.profile.application;

import com.chat.profile.dto.CreateProfileRequest;
import com.chat.profile.dto.ProfileResponse;
import com.chat.profile.dto.UpdateProfileRequest;

import java.util.List;

public interface ProfileManager {

    List<ProfileResponse> getMyProfiles(String userId);

    ProfileResponse createProfile(String userId, CreateProfileRequest request);

    /**
     * 예외: P001(없음), P002(본인 아님)
     * 사이드이펙트: 해당 프로필을 사용 중인 모든 채팅방에 PROFILE_UPDATED 브로드캐스트 (Redis 인프라 구성 후 추가)
     */
    ProfileResponse updateNickname(String userId, Long profileId, UpdateProfileRequest request);

    /**
     * 예외: P001(없음), P002(본인 아님), P003(채팅방에서 사용 중)
     */
    void deleteProfile(String userId, Long profileId);
}
