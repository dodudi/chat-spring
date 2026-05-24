package com.chat.message.application;

import com.chat.message.dto.RoomLastMessageDto;
import com.chat.message.infrastructure.LastMessageProjection;
import com.chat.message.infrastructure.MessageRepository;
import com.chat.message.infrastructure.UnreadCountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultMessageQueryService implements MessageQueryService {

    private final MessageRepository messageRepository;

    @Override
    public Map<UUID, RoomLastMessageDto> getLastMessages(Collection<UUID> roomIds) {
        return messageRepository.findLastMessages(roomIds).stream()
                .collect(Collectors.toMap(
                        LastMessageProjection::getRoomId,
                        p -> new RoomLastMessageDto(p.getRoomId(), p.getContent(), p.getCreatedAt())));
    }

    @Override
    public Map<UUID, Long> getUnreadCounts(Collection<UUID> roomIds, String userId) {
        return messageRepository.countUnreadByRoomIds(roomIds, userId).stream()
                .collect(Collectors.toMap(
                        UnreadCountProjection::getRoomId,
                        UnreadCountProjection::getUnreadCount));
    }
}
