package com.chat.user.application;

import com.chat.group.domain.UserGroup;
import com.chat.group.infrastructure.UserGroupRepository;
import com.chat.profile.domain.Profile;
import com.chat.profile.infrastructure.ProfileRepository;
import com.chat.user.domain.User;
import com.chat.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
        try {
            userRepository.save(User.create(userId));
            profileRepository.save(Profile.createDefault(userId));
            userGroupRepository.save(UserGroup.createDefault(userId));
            log.info("[USER_INIT] userId={}", userId);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 인한 중복 생성 시도 — 선행 트랜잭션이 이미 초기화함
            log.debug("[USER_INIT_SKIP] userId={} concurrent init ignored", userId);
        }
    }
}
