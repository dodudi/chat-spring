package kr.it.rudy.chat.user.application;

import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.user.domain.User;
import kr.it.rudy.chat.user.domain.UserRepository;
import kr.it.rudy.chat.user.domain.UserStatus;
import kr.it.rudy.chat.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SimpleUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SimpleUserService userService;

    @Test
    void findOrCreate_이미_존재하는_사용자_조회시_UserResponse_반환() {
        // given
        User user = User.create("ext-123");
        given(userRepository.findByExternalId("ext-123")).willReturn(Optional.of(user));

        // when
        UserResponse response = userService.findOrCreate("ext-123");

        // then
        assertThat(response.externalId()).isEqualTo("ext-123");
        assertThat(response.status()).isEqualTo(UserStatus.OFFLINE);
        then(userRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void findOrCreate_존재하지_않는_사용자_조회시_새로_생성하고_UserResponse_반환() {
        // given
        User user = User.create("ext-new");
        given(userRepository.findByExternalId("ext-new")).willReturn(Optional.empty());
        given(userRepository.save(any())).willReturn(user);

        // when
        UserResponse response = userService.findOrCreate("ext-new");

        // then
        assertThat(response.externalId()).isEqualTo("ext-new");
        then(userRepository).should().save(any());
    }

    @Test
    void findById_존재하는_사용자_조회시_UserResponse_반환() {
        // given
        User user = User.create("ext-123");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        UserResponse response = userService.findById(1L);

        // then
        assertThat(response.externalId()).isEqualTo("ext-123");
        assertThat(response.status()).isEqualTo(UserStatus.OFFLINE);
    }

    @Test
    void findById_존재하지_않는_id_조회시_AuthException_발생() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void updateStatus_존재하는_사용자_상태_변경시_UserResponse_반환() {
        // given
        User user = User.create("ext-123");
        given(userRepository.findByExternalId("ext-123")).willReturn(Optional.of(user));

        // when
        UserResponse response = userService.updateStatus("ext-123", UserStatus.ONLINE);

        // then
        assertThat(response.status()).isEqualTo(UserStatus.ONLINE);
    }

    @Test
    void updateStatus_존재하지_않는_사용자_상태_변경시_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-999")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateStatus("ext-999", UserStatus.ONLINE))
                .isInstanceOf(AuthException.class);
    }
}
