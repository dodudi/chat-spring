package com.chat.room.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultRoomPasswordEncoder implements RoomPasswordEncoder {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String encode(String rawPassword) {
        return rawPassword != null ? passwordEncoder.encode(rawPassword) : null;
    }
}
