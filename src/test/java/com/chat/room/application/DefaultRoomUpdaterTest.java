package com.chat.room.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.room.domain.ChatRoom;
import com.chat.room.dto.RoomResponse;
import com.chat.room.dto.UpdateRoomNameRequest;
import com.chat.room.dto.UpdateRoomPasswordRequest;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DefaultRoomUpdaterTest {

    @InjectMocks
    private DefaultRoomUpdater roomUpdater;

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private RoomPasswordEncoder roomPasswordEncoder;

    @Test
    @DisplayName("그룹 방 이름 수정 성공")
    void updateName_groupRoom_success() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup(userId, "기존이름", "key");
        UpdateRoomNameRequest request = new UpdateRoomNameRequest("새이름");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, userId)).willReturn(true);

        // when
        RoomResponse response = roomUpdater.updateName(userId, roomId, request);

        // then
        assertThat(response.name()).isEqualTo("새이름");
        assertThat(response.type()).isEqualTo("GROUP");
    }

    @Test
    @DisplayName("PUBLIC 방 이름 수정 성공")
    void updateName_publicRoom_success() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createPublic(userId, "기존이름", "hashed", "key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, userId)).willReturn(true);

        // when
        RoomResponse response = roomUpdater.updateName(userId, roomId, new UpdateRoomNameRequest("새이름"));

        // then
        assertThat(response.name()).isEqualTo("새이름");
        assertThat(response.type()).isEqualTo("PUBLIC");
    }

    @Test
    @DisplayName("존재하지 않는 방 이름 수정 시 예외 발생")
    void updateName_roomNotFound_throwsException() {
        // given
        UUID roomId = UUID.randomUUID();
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomUpdater.updateName("user-1", roomId, new UpdateRoomNameRequest("새이름")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("방장이 아닌 사용자가 이름 수정 시 예외 발생")
    void updateName_notOwner_throwsException() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup("owner", "기존이름", "key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> roomUpdater.updateName(userId, roomId, new UpdateRoomNameRequest("새이름")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_FORBIDDEN);
    }

    @Test
    @DisplayName("DM 방 이름 수정 시 예외 발생")
    void updateName_dmRoom_throwsException() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createDm(userId, "dm-key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, userId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> roomUpdater.updateName(userId, roomId, new UpdateRoomNameRequest("새이름")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_TYPE_UNSUPPORTED);
    }

    @Test
    @DisplayName("PUBLIC 방 비밀번호 수정 성공")
    void updatePassword_publicRoom_success() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createPublic(userId, "공개방", "old-hashed", "key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, userId)).willReturn(true);
        given(roomPasswordEncoder.encode("newpw")).willReturn("new-hashed");

        // when
        roomUpdater.updatePassword(userId, roomId, new UpdateRoomPasswordRequest("newpw"));

        // then
        assertThat(room.getPassword()).isEqualTo("new-hashed");
    }

    @Test
    @DisplayName("방장 아닌 사용자가 비밀번호 수정 시 예외 발생")
    void updatePassword_notOwner_throwsException() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createPublic("owner", "공개방", "hashed", "key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> roomUpdater.updatePassword(userId, roomId, new UpdateRoomPasswordRequest("newpw")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_FORBIDDEN);
    }

    @Test
    @DisplayName("GROUP 방에 비밀번호 수정 시 예외 발생")
    void updatePassword_groupRoom_throwsException() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup(userId, "그룹방", "key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, userId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> roomUpdater.updatePassword(userId, roomId, new UpdateRoomPasswordRequest("pw")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_TYPE_UNSUPPORTED);
    }

    @Test
    @DisplayName("PUBLIC 방 비밀번호 해제 성공")
    void clearPassword_publicRoom_success() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createPublic(userId, "공개방", "hashed", "key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, userId)).willReturn(true);

        // when
        roomUpdater.clearPassword(userId, roomId);

        // then
        assertThat(room.getPassword()).isNull();
    }

    @Test
    @DisplayName("방장 아닌 사용자가 비밀번호 해제 시 예외 발생")
    void clearPassword_notOwner_throwsException() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createPublic("owner", "공개방", "hashed", "key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> roomUpdater.clearPassword(userId, roomId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_FORBIDDEN);
    }

    @Test
    @DisplayName("GROUP 방 비밀번호 해제 시 예외 발생")
    void clearPassword_groupRoom_throwsException() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        ChatRoom room = ChatRoom.createGroup(userId, "그룹방", "key");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.isOwner(roomId, userId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> roomUpdater.clearPassword(userId, roomId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_TYPE_UNSUPPORTED);
    }
}
