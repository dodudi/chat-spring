package com.chat.message.application;

import com.chat.message.dto.RoomLastMessageDto;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface MessageQueryService {

    Map<UUID, RoomLastMessageDto> getLastMessages(Collection<UUID> roomIds);

    Map<UUID, Long> getUnreadCounts(Collection<UUID> roomIds, String userId);
}
