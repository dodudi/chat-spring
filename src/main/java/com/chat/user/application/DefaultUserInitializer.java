package com.chat.user.application;

import com.chat.group.domain.UserGroup;
import com.chat.group.infrastructure.UserGroupRepository;
import com.chat.profile.domain.Profile;
import com.chat.profile.infrastructure.ProfileRepository;
import com.chat.user.domain.User;
import com.chat.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DefaultUserInitializer implements UserInitializer {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final UserGroupRepository userGroupRepository;

    @Override
    @Transactional
    public void initUser(String userId) {
        if (userRepository.existsById(userId)) {
            return;
        }
        userRepository.save(User.create(userId));
        profileRepository.save(Profile.createDefault(userId));
        userGroupRepository.save(UserGroup.createDefault(userId));
        log.info("[USER_INIT] userId={}", userId);
    }
}
