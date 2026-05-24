package com.chat.invitelink.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.invitelink.domain.InviteLink;
import com.chat.invitelink.dto.CreateInviteLinkRequest;
import com.chat.invitelink.dto.InviteLinkResponse;
import com.chat.invitelink.infrastructure.InviteLinkRepository;
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
public class DefaultInviteLinkManager implements InviteLinkManager {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final InviteLinkRepository inviteLinkRepository;

    @Override
    public InviteLinkResponse createLink(String userId, UUID roomId, CreateInviteLinkRequest request) {
        var room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (room.getType() == RoomType.DM) {
            throw new AppException(ErrorCode.ROOM_TYPE_UNSUPPORTED);
        }

        if (!chatRoomMemberRepository.isOwner(roomId, userId)) {
            throw new AppException(ErrorCode.ROOM_FORBIDDEN);
        }

        InviteLink link = inviteLinkRepository.save(InviteLink.create(roomId, userId, request.expiresAt()));
        return InviteLinkResponse.from(link);
    }

    @Override
    public void deactivateLink(String userId, Long linkId) {
        InviteLink link = inviteLinkRepository.findById(linkId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!chatRoomMemberRepository.isOwner(link.getRoomId(), userId)) {
            throw new AppException(ErrorCode.ROOM_FORBIDDEN);
        }

        link.deactivate();
    }
}
