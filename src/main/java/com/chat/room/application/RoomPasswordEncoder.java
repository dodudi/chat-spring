package com.chat.room.application;

public interface RoomPasswordEncoder {

    /** rawPassword가 null이면 null 반환. */
    String encode(String rawPassword);

    /** rawPassword 또는 encodedPassword가 null이면 false 반환. */
    boolean matches(String rawPassword, String encodedPassword);
}
