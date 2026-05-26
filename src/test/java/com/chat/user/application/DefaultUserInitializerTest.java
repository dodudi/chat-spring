package com.chat.user.application;

import com.chat.group.domain.UserGroup;
import com.chat.group.infrastructure.UserGroupRepository;
import com.chat.profile.domain.Profile;
import com.chat.profile.infrastructure.ProfileRepository;
import com.chat.user.domain.User;
import com.chat.user.infrastructure.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultUserInitializerTest {

    @InjectMocks
    private DefaultUserInitializer userInitializer;

    @Mock private UserRepository userRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private UserGroupRepository userGroupRepository;

    @Test
    @DisplayName("신규 사용자 초기화 성공 — User·Profile·UserGroup 생성")
    void initUser_신규사용자_성공() {
        // given
        String userId = "new-user";
        given(userRepository.existsById(userId)).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));
        given(profileRepository.save(any(Profile.class))).willAnswer(inv -> inv.getArgument(0));
        given(userGroupRepository.save(any(UserGroup.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        userInitializer.initUser(userId);

        // then
        verify(userRepository).save(any(User.class));
        verify(profileRepository).save(any(Profile.class));
        verify(userGroupRepository).save(any(UserGroup.class));
    }

    @Test
    @DisplayName("이미 존재하는 사용자는 초기화 건너뜀")
    void initUser_이미존재_스킵() {
        // given
        String userId = "existing-user";
        given(userRepository.existsById(userId)).willReturn(true);

        // when
        userInitializer.initUser(userId);

        // then
        verify(userRepository, never()).save(any());
        verify(profileRepository, never()).save(any());
        verify(userGroupRepository, never()).save(any());
    }

    @Test
    @DisplayName("동시 요청으로 DataIntegrityViolationException 발생 시 무시")
    void initUser_동시요청_예외무시() {
        // given
        String userId = "new-user";
        given(userRepository.existsById(userId)).willReturn(false);
        given(userRepository.save(any(User.class))).willThrow(new DataIntegrityViolationException("duplicate"));

        // when & then
        assertThatNoException().isThrownBy(() -> userInitializer.initUser(userId));
    }
}
