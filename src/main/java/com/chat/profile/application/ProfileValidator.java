package com.chat.profile.application;

import com.chat.profile.domain.Profile;

public interface ProfileValidator {

    /** userId 소유 여부 검증 후 Profile 반환. 예외: P001(없음), P002(본인 아님) */
    Profile validateOwnership(String userId, Long profileId);
}
