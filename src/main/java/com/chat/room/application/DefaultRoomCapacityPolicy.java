package com.chat.room.application;

import com.chat.room.domain.RoomType;
import org.springframework.stereotype.Component;

@Component
public class DefaultRoomCapacityPolicy implements RoomCapacityPolicy {

    private static final int GROUP_MAX = 10;
    private static final int PUBLIC_MAX = 10;

    @Override
    public int maxCapacity(RoomType roomType) {
        return switch (roomType) {
            case GROUP -> GROUP_MAX;
            case PUBLIC -> PUBLIC_MAX;
            case DM -> throw new IllegalArgumentException("DM has no capacity limit");
        };
    }
}
