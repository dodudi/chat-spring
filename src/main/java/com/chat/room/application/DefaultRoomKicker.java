package com.chat.room.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.domain.RoomType;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DefaultRoomKicker implements RoomKicker {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    public void kickMember(String requesterId, UUID roomId, String targetUserId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        if (room.getType() == RoomType.DM) {
            throw new AppException(ErrorCode.ROOM_TYPE_UNSUPPORTED);
        }
        if (!chatRoomMemberRepository.isOwner(roomId, requesterId)) {
            throw new AppException(ErrorCode.ROOM_FORBIDDEN);
        }
        if (requesterId.equals(targetUserId)) {
            throw new AppException(ErrorCode.ROOM_SELF_KICK);
        }
        ChatRoomMember target = chatRoomMemberRepository.findByRoomIdAndUserId(roomId, targetUserId)
                .filter(m -> m.getKickedAt() == null && m.getLeftAt() == null)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_MEMBER_NOT_FOUND));
        target.kick();
    }
}
