package com.chat.room.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.message.application.MessageQueryService;
import com.chat.profile.infrastructure.ProfileRepository;
import com.chat.message.dto.RoomLastMessageDto;
import com.chat.room.domain.ChatRoom;
import com.chat.room.dto.RoomDetailResponse;
import com.chat.room.dto.RoomSummaryResponse;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DefaultRoomReaderTest {

    @InjectMocks
    private DefaultRoomReader roomReader;

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private MessageQueryService messageQueryService;

    @Test
    @DisplayName("존재하지 않는 채팅방 조회 시 예외 발생")
    void getRoomDetail_roomNotFound_throwsException() {
        // given
        UUID roomId = UUID.randomUUID();
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomReader.getRoomDetail("user-1", roomId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("채팅방 멤버가 아닌 사용자가 그룹 방 조회 시 예외 발생")
    void getRoomDetail_nonMemberAccessingGroupRoom_throwsException() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup("owner", "테스트방", "key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.existsActiveMember(roomId, userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> roomReader.getRoomDetail(userId, roomId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("채팅방 멤버가 그룹 방 조회 성공")
    void getRoomDetail_memberAccessingGroupRoom_returnsDetail() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup("owner", "테스트방", "key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.existsActiveMember(roomId, userId)).willReturn(true);
        given(chatRoomMemberRepository.countByRoomId(roomId)).willReturn(3L);

        // when
        RoomDetailResponse response = roomReader.getRoomDetail(userId, roomId);

        // then
        assertThat(response.name()).isEqualTo("테스트방");
        assertThat(response.type()).isEqualTo("GROUP");
        assertThat(response.memberCount()).isEqualTo(3L);
        assertThat(response.hasPassword()).isFalse();
    }

    @Test
    @DisplayName("PUBLIC 방은 멤버가 아니어도 조회 성공")
    void getRoomDetail_nonMemberAccessingPublicRoom_returnsDetail() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createPublic("owner", "공개방", "hashed-pw", "key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.countByRoomId(roomId)).willReturn(5L);

        // when
        RoomDetailResponse response = roomReader.getRoomDetail(userId, roomId);

        // then
        assertThat(response.type()).isEqualTo("PUBLIC");
        assertThat(response.hasPassword()).isTrue();
        assertThat(response.memberCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("참여 중인 채팅방이 없으면 빈 목록 반환")
    void getMyRooms_noRooms_returnsEmpty() {
        // given
        given(chatRoomRepository.findMyRooms("user-1", null)).willReturn(List.of());

        // when
        var result = roomReader.getMyRooms("user-1", null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("그룹 방 목록 조회 성공 - 이름·타입 포함")
    void getMyRooms_withGroupRoom_returnsSummary() {
        // given
        String userId = "user-1";
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");

        given(chatRoomRepository.findMyRooms(userId, null)).willReturn(List.of(room));
        given(messageQueryService.getLastMessages(any())).willReturn(new HashMap<>());
        given(messageQueryService.getUnreadCounts(any(), any())).willReturn(new HashMap<>());

        // when
        List<RoomSummaryResponse> result = roomReader.getMyRooms(userId, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("그룹방");
        assertThat(result.get(0).type()).isEqualTo("GROUP");
        assertThat(result.get(0).unreadCount()).isZero();
    }
}
