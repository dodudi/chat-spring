package com.chat.room.application;

public interface RoomPasswordEncoder {

    /** rawPassword가 null이면 null 반환. */
    String encode(String rawPassword);
}
