package com.chat.message.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.message.domain.Message;
import com.chat.message.dto.MessageCursorResponse;
import com.chat.message.dto.MessageResponse;
import com.chat.message.infrastructure.MessageRepository;
import com.chat.profile.infrastructure.ProfileRepository;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultMessageReader implements MessageReader {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MessageRepository messageRepository;
    private final ProfileRepository profileRepository;

    @Override
    public MessageCursorResponse getHistory(String userId, UUID roomId, Long cursor, int size) {
        chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        ChatRoomMember member = chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .filter(m -> m.getLeftAt() == null && m.getKickedAt() == null)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_MEMBER_NOT_FOUND));

        // DM 숨김 해제 이전 메시지 접근 차단
        OffsetDateTime hiddenAfter = member.isHidden() ? member.getHiddenAt() : null;

        List<Message> messages = messageRepository.findHistory(roomId, cursor, hiddenAfter, PageRequest.of(0, size));

        List<Long> profileIds = messages.stream()
                .map(Message::getProfileId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> nicknameMap = profileRepository.findAllById(profileIds).stream()
                .collect(Collectors.toMap(p -> p.getId(), p -> p.getNickname()));

        List<MessageResponse> responses = messages.stream()
                .map(m -> MessageResponse.of(m, nicknameMap.getOrDefault(m.getProfileId(), "")))
                .toList();

        Long nextCursor = responses.size() == size ? responses.get(responses.size() - 1).id() : null;
        return new MessageCursorResponse(responses, nextCursor);
    }
}
