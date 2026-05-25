package com.chat.message.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.message.domain.Message;
import com.chat.message.dto.MessageCursorResponse;
import com.chat.message.infrastructure.MessageRepository;
import com.chat.profile.infrastructure.ProfileRepository;
import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.domain.MemberRole;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultMessageReaderTest {

    @InjectMocks
    private DefaultMessageReader messageReader;

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private ProfileRepository profileRepository;

    @Test
    @DisplayName("메시지 조회 성공 — 메시지 목록과 nextCursor 반환")
    void getHistory_success() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup(userId, "그룹방", "key");
        ChatRoomMember member = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);

        Message msg1 = Message.create(roomId, userId, 1L, "첫 번째");
        Message msg2 = Message.create(roomId, userId, 1L, "두 번째");
        ReflectionTestUtils.setField(msg1, "id", 2L);
        ReflectionTestUtils.setField(msg2, "id", 1L);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));
        given(messageRepository.findHistory(eq(roomId), isNull(), isNull(), any(Pageable.class)))
                .willReturn(List.of(msg1, msg2));
        given(profileRepository.findAllById(any())).willReturn(List.of());

        // when
        MessageCursorResponse response = messageReader.getHistory(userId, roomId, null, 2);

        // then
        assertThat(response.messages()).hasSize(2);
        assertThat(response.nextCursor()).isEqualTo(1L);
    }

    @Test
    @DisplayName("마지막 페이지 조회 — nextCursor null 반환")
    void getHistory_lastPage_nextCursorNull() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup(userId, "그룹방", "key");
        ChatRoomMember member = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);
        Message msg = Message.create(roomId, userId, 1L, "마지막 메시지");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));
        given(messageRepository.findHistory(any(), any(), any(), any(Pageable.class)))
                .willReturn(List.of(msg));
        given(profileRepository.findAllById(any())).willReturn(List.of());

        // when
        MessageCursorResponse response = messageReader.getHistory(userId, roomId, null, 2);

        // then
        assertThat(response.messages()).hasSize(1);
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 방 메시지 조회 시 예외 발생")
    void getHistory_roomNotFound_throwsException() {
        // given
        UUID roomId = UUID.randomUUID();
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> messageReader.getHistory("user-1", roomId, null, 20))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("채팅방 멤버가 아닌 사용자가 조회 시 예외 발생")
    void getHistory_nonMember_throwsException() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> messageReader.getHistory(userId, roomId, null, 20))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("퇴장한 멤버가 메시지 조회 시 예외 발생")
    void getHistory_leftMember_throwsException() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");
        ChatRoomMember member = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);
        member.leave();

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> messageReader.getHistory(userId, roomId, null, 20))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("강퇴된 멤버가 메시지 조회 시 예외 발생")
    void getHistory_kickedMember_throwsException() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");
        ChatRoomMember member = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);
        member.kick();

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> messageReader.getHistory(userId, roomId, null, 20))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("cursor 값으로 연속 페이징 조회 — cursor 파라미터가 findHistory에 전달됨")
    void getHistory_withCursor_passedToRepository() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        Long cursor = 5L;
        ChatRoom room = ChatRoom.createGroup(userId, "그룹방", "key");
        ChatRoomMember member = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));
        given(messageRepository.findHistory(eq(roomId), eq(cursor), isNull(), any(Pageable.class)))
                .willReturn(List.of());
        given(profileRepository.findAllById(any())).willReturn(List.of());

        // when
        messageReader.getHistory(userId, roomId, cursor, 20);

        // then
        verify(messageRepository).findHistory(eq(roomId), eq(cursor), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("메시지가 없는 방 조회 — 빈 목록과 nextCursor null 반환")
    void getHistory_emptyRoom_returnsEmptyList() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup(userId, "그룹방", "key");
        ChatRoomMember member = ChatRoomMember.create(roomId, userId, 1L, MemberRole.MEMBER);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));
        given(messageRepository.findHistory(any(), any(), any(), any(Pageable.class))).willReturn(List.of());
        given(profileRepository.findAllById(any())).willReturn(List.of());

        // when
        MessageCursorResponse response = messageReader.getHistory(userId, roomId, null, 20);

        // then
        assertThat(response.messages()).isEmpty();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("DM 숨김 멤버 조회 — hiddenAt 이후 메시지만 반환")
    void getHistory_dmHiddenMember_filtersFromHiddenAt() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createDm(userId, "dm-key");
        ChatRoomMember member = ChatRoomMember.create(roomId, userId, 1L, MemberRole.OWNER);
        member.hide();

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));
        given(messageRepository.findHistory(eq(roomId), isNull(), notNull(), any(Pageable.class)))
                .willReturn(List.of());
        given(profileRepository.findAllById(any())).willReturn(List.of());

        // when
        messageReader.getHistory(userId, roomId, null, 20);

        // then
        verify(messageRepository).findHistory(eq(roomId), isNull(), eq(member.getHiddenAt()), any(Pageable.class));
    }
}
