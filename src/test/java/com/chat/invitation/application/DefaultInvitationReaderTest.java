package com.chat.invitation.application;

import com.chat.invitation.domain.Invitation;
import com.chat.invitation.dto.InvitationResponse;
import com.chat.invitation.infrastructure.InvitationRepository;
import com.chat.room.domain.ChatRoom;
import com.chat.room.infrastructure.ChatRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DefaultInvitationReaderTest {

    @InjectMocks
    private DefaultInvitationReader invitationReader;

    @Mock private InvitationRepository invitationRepository;
    @Mock private ChatRoomRepository chatRoomRepository;

    @Test
    @DisplayName("대기 중인 초대 목록 조회 성공")
    void getPendingInvitations_성공() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        Invitation invitation = Invitation.create(roomId, "owner", userId);
        ChatRoom room = ChatRoom.createGroup("owner", "그룹방", "key");
        // ChatRoom.id는 @GeneratedValue라 단위테스트에서 null — 설정 필요
        ReflectionTestUtils.setField(room, "id", roomId);

        given(invitationRepository.findPendingByInviteeId(userId)).willReturn(List.of(invitation));
        given(chatRoomRepository.findAllById(any())).willReturn(List.of(room));

        // when
        List<InvitationResponse> responses = invitationReader.getPendingInvitations(userId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).roomName()).isEqualTo("그룹방");
        assertThat(responses.get(0).inviterId()).isEqualTo("owner");
    }

    @Test
    @DisplayName("대기 중인 초대가 없으면 빈 목록 반환")
    void getPendingInvitations_빈목록() {
        // given
        String userId = "user-1";
        given(invitationRepository.findPendingByInviteeId(userId)).willReturn(List.of());

        // when
        List<InvitationResponse> responses = invitationReader.getPendingInvitations(userId);

        // then
        assertThat(responses).isEmpty();
    }
}
