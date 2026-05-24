package com.chat.room.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.RoomType;
import com.chat.room.dto.RoomResponse;
import com.chat.room.dto.UpdateRoomNameRequest;
import com.chat.room.dto.UpdateRoomPasswordRequest;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DefaultRoomUpdater implements RoomUpdater {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final RoomPasswordEncoder roomPasswordEncoder;

    @Override
    public RoomResponse updateName(String userId, UUID roomId, UpdateRoomNameRequest request) {
        ChatRoom room = findAndValidateOwner(userId, roomId);
        if (room.getType() == RoomType.DM) {
            throw new AppException(ErrorCode.ROOM_TYPE_UNSUPPORTED);
        }
        room.updateName(request.name());
        return RoomResponse.from(room);
    }

    @Override
    public RoomResponse updatePassword(String userId, UUID roomId, UpdateRoomPasswordRequest request) {
        ChatRoom room = findAndValidateOwner(userId, roomId);
        if (room.getType() != RoomType.PUBLIC) {
            throw new AppException(ErrorCode.ROOM_TYPE_UNSUPPORTED);
        }
        room.updatePassword(roomPasswordEncoder.encode(request.password()));
        return RoomResponse.from(room);
    }

    @Override
    public void clearPassword(String userId, UUID roomId) {
        ChatRoom room = findAndValidateOwner(userId, roomId);
        if (room.getType() != RoomType.PUBLIC) {
            throw new AppException(ErrorCode.ROOM_TYPE_UNSUPPORTED);
        }
        room.updatePassword(null);
    }

    private ChatRoom findAndValidateOwner(String userId, UUID roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        if (!chatRoomMemberRepository.isOwner(roomId, userId)) {
            throw new AppException(ErrorCode.ROOM_FORBIDDEN);
        }
        return room;
    }
}
