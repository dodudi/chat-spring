package com.chat.room.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.domain.RoomType;
import com.chat.room.dto.CreateDmRoomRequest;
import com.chat.room.dto.CreateGroupRoomRequest;
import com.chat.room.dto.InviteMemberRequest;
import com.chat.room.dto.RoomSummaryResponse;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import com.chat.room.infrastructure.RoomSummaryProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock ChatRoomRepository chatRoomRepository;
    @Mock ChatRoomMemberRepository chatRoomMemberRepository;
    @InjectMocks RoomServiceImpl roomService;

    @Test
    void createOrGetDmRoom_기존_방이_있으면_기존_방_반환() {
        // given
        ChatRoom existing = dmRoom(1L);
        RoomSummaryProjection projection = stubProjection(1L, "DM", null, "user-a", "user-b");

        given(chatRoomRepository.findByDmUserAAndDmUserBAndType("user-a", "user-b", RoomType.DM))
                .willReturn(Optional.of(existing));
        given(chatRoomMemberRepository.findMyRoomsWithUnread("user-a"))
                .willReturn(List.of(projection));

        // when
        RoomSummaryResponse result = roomService.createOrGetDmRoom("user-a", new CreateDmRoomRequest("user-b"));

        // then
        assertThat(result.type()).isEqualTo(RoomType.DM);
        assertThat(result.dmTargetUserId()).isEqualTo("user-b");
        then(chatRoomRepository).should(never()).save(any());
    }

    @Test
    void createOrGetDmRoom_기존_방이_없으면_신규_생성() {
        // given
        ChatRoom newRoom = ChatRoom.createDm("user-a", "user-b");
        given(chatRoomRepository.findByDmUserAAndDmUserBAndType(anyString(), anyString(), eq(RoomType.DM)))
                .willReturn(Optional.empty());
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(newRoom);
        given(chatRoomMemberRepository.save(any(ChatRoomMember.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        RoomSummaryResponse result = roomService.createOrGetDmRoom("user-a", new CreateDmRoomRequest("user-b"));

        // then
        assertThat(result.type()).isEqualTo(RoomType.DM);
        then(chatRoomMemberRepository).should(times(2)).save(any(ChatRoomMember.class));
    }

    @Test
    void createGroupRoom_방과_멤버_생성() {
        // given
        ChatRoom room = ChatRoom.createGroup("user-a", "팀 채팅");
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(room);
        given(chatRoomMemberRepository.save(any(ChatRoomMember.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        RoomSummaryResponse result = roomService.createGroupRoom(
                "user-a", new CreateGroupRoomRequest("팀 채팅", List.of("user-b", "user-c")));

        // then
        assertThat(result.type()).isEqualTo(RoomType.GROUP);
        assertThat(result.name()).isEqualTo("팀 채팅");
        then(chatRoomMemberRepository).should(times(3)).save(any(ChatRoomMember.class)); // 생성자 + 2명
    }

    @Test
    void getMyRooms_projection_목록_반환() {
        // given
        RoomSummaryProjection p = stubProjection(1L, "GROUP", "팀 채팅", null, null);
        given(chatRoomMemberRepository.findMyRoomsWithUnread("user-a")).willReturn(List.of(p));

        // when
        List<RoomSummaryResponse> result = roomService.getMyRooms("user-a");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("팀 채팅");
    }

    @Test
    void inviteMembers_DM방이면_R002_예외() {
        // given
        ChatRoom dmRoom = ChatRoom.createDm("user-a", "user-b");
        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(dmRoom));

        // when & then
        assertThatThrownBy(() -> roomService.inviteMembers("user-a", 1L, new InviteMemberRequest(List.of("user-c"))))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_ACCESS_DENIED);
    }

    @Test
    void inviteMembers_비활성_멤버는_재가입_처리() {
        // given
        ChatRoom room = ChatRoom.createGroup("user-a", "팀");
        ChatRoomMember inactive = ChatRoomMember.create(room, "user-b");
        inactive.leave();

        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoom_IdAndUserId(1L, "user-b"))
                .willReturn(Optional.of(inactive));

        // when
        roomService.inviteMembers("user-a", 1L, new InviteMemberRequest(List.of("user-b")));

        // then
        assertThat(inactive.isActive()).isTrue();
    }

    @Test
    void leaveRoom_활성_멤버가_나가면_비활성_처리() {
        // given
        ChatRoom room = ChatRoom.createGroup("user-a", "팀");
        ChatRoomMember member = ChatRoomMember.create(room, "user-a");
        given(chatRoomMemberRepository.findByRoom_IdAndUserId(1L, "user-a"))
                .willReturn(Optional.of(member));

        // when
        roomService.leaveRoom("user-a", 1L);

        // then
        assertThat(member.isActive()).isFalse();
    }

    @Test
    void leaveRoom_멤버가_없으면_R002_예외() {
        // given
        given(chatRoomMemberRepository.findByRoom_IdAndUserId(1L, "user-a"))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomService.leaveRoom("user-a", 1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_ACCESS_DENIED);
    }

    private ChatRoom dmRoom(Long id) {
        ChatRoom room = ChatRoom.createDm("user-a", "user-b");
        try {
            var f = ChatRoom.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(room, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return room;
    }

    private RoomSummaryProjection stubProjection(Long id, String type, String name, String dmUserA, String dmUserB) {
        return new RoomSummaryProjection() {
            @Override public Long getId() { return id; }
            @Override public String getType() { return type; }
            @Override public String getName() { return name; }
            @Override public String getDmUserA() { return dmUserA; }
            @Override public String getDmUserB() { return dmUserB; }
            @Override public OffsetDateTime getUpdatedAt() { return OffsetDateTime.now(); }
            @Override public String getLastMessageContent() { return null; }
            @Override public OffsetDateTime getLastMessageAt() { return null; }
            @Override public long getUnreadCount() { return 0L; }
        };
    }
}
