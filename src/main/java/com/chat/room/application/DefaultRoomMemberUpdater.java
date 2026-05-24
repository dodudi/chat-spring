package com.chat.room.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.profile.application.ProfileValidator;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DefaultRoomMemberUpdater implements RoomMemberUpdater {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ProfileValidator profileValidator;

    @Override
    public void updateProfile(String userId, UUID roomId, Long profileId) {
        ChatRoomMember member = chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .filter(m -> m.getLeftAt() == null && m.getKickedAt() == null)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_MEMBER_NOT_FOUND));

        profileValidator.validateOwnership(userId, profileId);
        member.updateProfileId(profileId);
    }
}
