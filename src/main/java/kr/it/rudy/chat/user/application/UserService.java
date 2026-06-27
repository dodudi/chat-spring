package kr.it.rudy.chat.user.application;

import kr.it.rudy.chat.user.domain.UserStatus;
import kr.it.rudy.chat.user.dto.UserResponse;

public interface UserService {
    UserResponse findOrCreate(String externalId);

    UserResponse findById(Long id);

    UserResponse updateStatus(String externalId, UserStatus status);
}
