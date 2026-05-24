package com.chat.room.application;

import com.chat.common.dto.PageResponse;
import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.message.application.MessageQueryService;
import com.chat.message.dto.RoomLastMessageDto;
import com.chat.profile.domain.Profile;
import com.chat.profile.infrastructure.ProfileRepository;
import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.RoomType;
import com.chat.room.dto.PublicRoomSummaryResponse;
import com.chat.room.dto.RoomDetailResponse;
import com.chat.room.dto.RoomSummaryResponse;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import com.chat.room.infrastructure.DmRoomNameProjection;
import com.chat.room.infrastructure.RoomMemberCountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultRoomReader implements RoomReader {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ProfileRepository profileRepository;
    private final MessageQueryService messageQueryService;

    @Override
    public List<RoomSummaryResponse> getMyRooms(String userId, Long groupId) {
        List<ChatRoom> rooms = chatRoomRepository.findMyRooms(userId, groupId);
        if (rooms.isEmpty()) {
            return List.of();
        }

        List<UUID> roomIds = rooms.stream().map(ChatRoom::getId).toList();
        List<UUID> dmRoomIds = rooms.stream()
                .filter(r -> r.getType() == RoomType.DM)
                .map(ChatRoom::getId)
                .toList();

        Map<UUID, String> dmNames = dmRoomIds.isEmpty() ? Map.of() :
                chatRoomMemberRepository.findDmRoomNames(dmRoomIds, userId).stream()
                        .collect(Collectors.toMap(DmRoomNameProjection::getRoomId, DmRoomNameProjection::getNickname));

        Map<UUID, RoomLastMessageDto> lastMessages = messageQueryService.getLastMessages(roomIds);
        Map<UUID, Long> unreadCounts = messageQueryService.getUnreadCounts(roomIds, userId);

        return rooms.stream()
                .map(room -> {
                    RoomLastMessageDto last = lastMessages.get(room.getId());
                    return new RoomSummaryResponse(
                            room.getId(),
                            room.getType().name(),
                            room.getType() == RoomType.DM
                                    ? dmNames.getOrDefault(room.getId(), "알 수 없음")
                                    : room.getName(),
                            last != null ? last.content() : null,
                            last != null ? last.createdAt() : null,
                            unreadCounts.getOrDefault(room.getId(), 0L).intValue(),
                            room.getUpdatedAt());
                })
                .toList();
    }

    @Override
    public RoomDetailResponse getRoomDetail(String userId, UUID roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        if (room.getType() != RoomType.PUBLIC && !chatRoomMemberRepository.existsActiveMember(roomId, userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        long memberCount = chatRoomMemberRepository.countByRoomId(roomId);
        return new RoomDetailResponse(
                room.getId(),
                room.getType().name(),
                resolveName(room, userId),
                room.getPassword() != null,
                memberCount,
                room.getCreatedAt());
    }

    @Override
    public PageResponse<PublicRoomSummaryResponse> searchPublicRooms(String name, int page, int size) {
        Page<ChatRoom> result = chatRoomRepository.searchPublicRooms(name, PageRequest.of(page, size));
        List<UUID> roomIds = result.getContent().stream().map(ChatRoom::getId).toList();
        Map<UUID, Long> countMap = chatRoomMemberRepository.countByRoomIds(roomIds).stream()
                .collect(Collectors.toMap(RoomMemberCountProjection::getRoomId, RoomMemberCountProjection::getMemberCount));
        List<PublicRoomSummaryResponse> content = result.getContent().stream()
                .map(room -> new PublicRoomSummaryResponse(
                        room.getId(),
                        room.getName(),
                        countMap.getOrDefault(room.getId(), 0L),
                        room.getPassword() != null))
                .toList();
        return new PageResponse<>(content, page, size, result.getTotalElements());
    }

    private String resolveName(ChatRoom room, String userId) {
        if (room.getType() != RoomType.DM) {
            return room.getName();
        }
        return chatRoomMemberRepository.findOtherMember(room.getId(), userId)
                .flatMap(other -> profileRepository.findById(other.getProfileId()))
                .map(Profile::getNickname)
                .orElse("알 수 없음");
    }
}
