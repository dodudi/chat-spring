package com.chat.room.application;

import com.chat.room.domain.RoomType;

public interface RoomCapacityPolicy {

    /** RoomType별 최대 참여 인원 반환. DM은 해당 없음. */
    int maxCapacity(RoomType roomType);
}
