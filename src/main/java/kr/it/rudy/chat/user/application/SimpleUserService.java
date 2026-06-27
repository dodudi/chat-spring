package kr.it.rudy.chat.user.application;

import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
import kr.it.rudy.chat.user.domain.User;
import kr.it.rudy.chat.user.domain.UserRepository;
import kr.it.rudy.chat.user.domain.UserStatus;
import kr.it.rudy.chat.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SimpleUserService implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserResponse findOrCreate(String externalId) {
        return userRepository.findByExternalId(externalId)
                .map(UserResponse::from)
                .orElseGet(() -> {
                    log.info("[USER_CREATE] externalId={}", externalId);
                    return UserResponse.from(userRepository.save(User.create(externalId)));
                });
    }

    @Override
    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional
    public UserResponse updateStatus(String externalId, UserStatus status) {
        User user = userRepository.findByExternalId(externalId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        user.updateStatus(status);
        log.info("[USER_STATUS_UPDATE] externalId={} status={}", externalId, status);
        return UserResponse.from(user);
    }
}
