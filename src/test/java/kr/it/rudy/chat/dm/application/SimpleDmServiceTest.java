package kr.it.rudy.chat.dm.application;

import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
import kr.it.rudy.chat.dm.domain.*;
import kr.it.rudy.chat.dm.dto.*;
import kr.it.rudy.chat.user.domain.User;
import kr.it.rudy.chat.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SimpleDmServiceTest {

    @Mock private DmChannelRepository channelRepository;
    @Mock private DmChannelParticipantRepository participantRepository;
    @Mock private DmMessageRepository messageRepository;
    @Mock private DmReadStatusRepository readStatusRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private SimpleDmService dmService;

    private User userA;
    private User userB;
    private DmChannel directChannel;
    private DmChannel groupChannel;
    private DmMessage message;

    @BeforeEach
    void setUp() {
        userA = User.create("ext-a");
        ReflectionTestUtils.setField(userA, "id", 1L);

        userB = User.create("ext-b");
        ReflectionTestUtils.setField(userB, "id", 2L);

        directChannel = DmChannel.createDirect();
        ReflectionTestUtils.setField(directChannel, "id", 10L);

        groupChannel = DmChannel.createGroup("테스트 그룹", null);
        ReflectionTestUtils.setField(groupChannel, "id", 20L);

        message = DmMessage.create(directChannel, userA, "안녕하세요", null);
        ReflectionTestUtils.setField(message, "id", 100L);
    }

    @Test
    void createDirectChannel_기존_채널이_없으면_새_채널_생성() {
        // given
        given(userRepository.findByExternalId("ext-a")).willReturn(Optional.of(userA));
        given(userRepository.findById(2L)).willReturn(Optional.of(userB));
        given(participantRepository.findExistingChannel(1L, 2L, DmChannelType.DIRECT)).willReturn(Optional.empty());
        given(channelRepository.save(any())).willReturn(directChannel);
        given(participantRepository.save(any())).willReturn(null);

        // when
        DmChannelResponse response = dmService.createDirectChannel("ext-a", 2L);

        // then
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.type()).isEqualTo(DmChannelType.DIRECT);
        then(channelRepository).should().save(any(DmChannel.class));
    }

    @Test
    void createDirectChannel_기존_채널이_있으면_기존_채널_반환() {
        // given
        given(userRepository.findByExternalId("ext-a")).willReturn(Optional.of(userA));
        given(userRepository.findById(2L)).willReturn(Optional.of(userB));
        given(participantRepository.findExistingChannel(1L, 2L, DmChannelType.DIRECT)).willReturn(Optional.of(directChannel));

        // when
        DmChannelResponse response = dmService.createDirectChannel("ext-a", 2L);

        // then
        assertThat(response.id()).isEqualTo(10L);
        then(channelRepository).shouldHaveNoInteractions();
    }

    @Test
    void createGroupChannel_정상_생성시_DmChannelResponse_반환() {
        // given
        given(userRepository.findByExternalId("ext-a")).willReturn(Optional.of(userA));
        given(channelRepository.save(any())).willReturn(groupChannel);
        given(userRepository.findById(2L)).willReturn(Optional.of(userB));
        given(participantRepository.save(any())).willReturn(null);

        // when
        DmChannelResponse response = dmService.createGroupChannel("ext-a",
                new CreateGroupDmRequest("테스트 그룹", null, List.of(2L)));

        // then
        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.type()).isEqualTo(DmChannelType.GROUP);
    }

    @Test
    void getMyChannels_참여_중인_채널_목록_반환() {
        // given
        DmChannelParticipant participant = DmChannelParticipant.create(directChannel, userA);
        given(userRepository.findByExternalId("ext-a")).willReturn(Optional.of(userA));
        given(participantRepository.findByUserIdAndLeftAtIsNull(1L)).willReturn(List.of(participant));

        // when
        List<DmChannelResponse> responses = dmService.getMyChannels("ext-a");

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(10L);
    }

    @Test
    void addParticipant_1대1_DM에_추가하면_AuthException_발생() {
        // given
        given(channelRepository.findById(10L)).willReturn(Optional.of(directChannel));

        // when & then
        assertThatThrownBy(() -> dmService.addParticipant("ext-a", 10L, 2L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.DM_DIRECT_CANNOT_ADD_PARTICIPANT.getMessage());
    }

    @Test
    void addParticipant_참여자가_아닌_상태에서_추가하면_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-a")).willReturn(Optional.of(userA));
        given(channelRepository.findById(20L)).willReturn(Optional.of(groupChannel));
        given(participantRepository.existsByDmChannelIdAndUserIdAndLeftAtIsNull(20L, 1L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> dmService.addParticipant("ext-a", 20L, 2L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.DM_NOT_PARTICIPANT.getMessage());
    }

    @Test
    void leaveChannel_정상_나가기() {
        // given
        DmChannelParticipant participant = DmChannelParticipant.create(directChannel, userA);
        given(userRepository.findByExternalId("ext-a")).willReturn(Optional.of(userA));
        given(participantRepository.findByDmChannelIdAndUserIdAndLeftAtIsNull(10L, 1L))
                .willReturn(Optional.of(participant));

        // when
        dmService.leaveChannel("ext-a", 10L);

        // then
        assertThat(participant.hasLeft()).isTrue();
    }

    @Test
    void leaveChannel_참여자가_아니면_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-a")).willReturn(Optional.of(userA));
        given(participantRepository.findByDmChannelIdAndUserIdAndLeftAtIsNull(10L, 1L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> dmService.leaveChannel("ext-a", 10L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.DM_NOT_PARTICIPANT.getMessage());
    }

    @Test
    void sendMessage_참여자가_메시지_전송시_DmMessageResponse_반환() {
        // given
        given(userRepository.findByExternalId("ext-a")).willReturn(Optional.of(userA));
        given(channelRepository.findById(10L)).willReturn(Optional.of(directChannel));
        given(participantRepository.existsByDmChannelIdAndUserIdAndLeftAtIsNull(10L, 1L)).willReturn(true);
        given(messageRepository.save(any())).willReturn(message);

        // when
        DmMessageResponse response = dmService.sendMessage("ext-a", 10L,
                new SendDmMessageRequest("안녕하세요", null));

        // then
        assertThat(response.content()).isEqualTo("안녕하세요");
        assertThat(response.senderId()).isEqualTo(1L);
    }

    @Test
    void sendMessage_참여자가_아니면_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-a")).willReturn(Optional.of(userA));
        given(channelRepository.findById(10L)).willReturn(Optional.of(directChannel));
        given(participantRepository.existsByDmChannelIdAndUserIdAndLeftAtIsNull(10L, 1L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> dmService.sendMessage("ext-a", 10L,
                new SendDmMessageRequest("안녕하세요", null)))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.DM_NOT_PARTICIPANT.getMessage());
    }

    @Test
    void findMessages_커서_없이_최신_메시지_반환() {
        // given
        given(channelRepository.existsById(10L)).willReturn(true);
        given(messageRepository.findByDmChannelIdAndDeletedAtIsNullOrderByIdDesc(any(Long.class), any(Pageable.class)))
                .willReturn(List.of(message));

        // when
        List<DmMessageResponse> responses = dmService.findMessages(10L, null, 50);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).content()).isEqualTo("안녕하세요");
    }

    @Test
    void findMessages_채널이_없으면_AuthException_발생() {
        // given
        given(channelRepository.existsById(999L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> dmService.findMessages(999L, null, 50))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.DM_CHANNEL_NOT_FOUND.getMessage());
    }

    @Test
    void editMessage_발신자가_수정시_수정된_DmMessageResponse_반환() {
        // given
        given(userRepository.findByExternalId("ext-a")).willReturn(Optional.of(userA));
        given(messageRepository.findById(100L)).willReturn(Optional.of(message));

        // when
        DmMessageResponse response = dmService.editMessage("ext-a", 100L,
                new EditDmMessageRequest("수정된 내용"));

        // then
        assertThat(response.content()).isEqualTo("수정된 내용");
        assertThat(response.isEdited()).isTrue();
    }

    @Test
    void editMessage_발신자가_아니면_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-b")).willReturn(Optional.of(userB));
        given(messageRepository.findById(100L)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> dmService.editMessage("ext-b", 100L,
                new EditDmMessageRequest("수정된 내용")))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.DM_MESSAGE_EDIT_FORBIDDEN.getMessage());
    }

    @Test
    void deleteMessage_발신자가_삭제시_소프트_삭제() {
        // given
        given(userRepository.findByExternalId("ext-a")).willReturn(Optional.of(userA));
        given(messageRepository.findById(100L)).willReturn(Optional.of(message));

        // when
        dmService.deleteMessage("ext-a", 100L);

        // then
        assertThat(message.isDeleted()).isTrue();
        assertThat(message.getDeletedAt()).isNotNull();
    }

    @Test
    void markRead_최초_읽음_처리시_DmReadStatus_생성() {
        // given
        given(userRepository.findByExternalId("ext-a")).willReturn(Optional.of(userA));
        given(participantRepository.existsByDmChannelIdAndUserIdAndLeftAtIsNull(10L, 1L)).willReturn(true);
        given(channelRepository.findById(10L)).willReturn(Optional.of(directChannel));
        given(messageRepository.findById(100L)).willReturn(Optional.of(message));
        given(readStatusRepository.findByDmChannelIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        // when
        dmService.markRead("ext-a", 10L, 100L);

        // then
        then(readStatusRepository).should().save(any(DmReadStatus.class));
    }

    @Test
    void markRead_기존_읽음_상태_업데이트() {
        // given
        DmReadStatus status = DmReadStatus.create(directChannel, userA, message);
        DmMessage newMessage = DmMessage.create(directChannel, userB, "새 메시지", null);
        ReflectionTestUtils.setField(newMessage, "id", 200L);

        given(userRepository.findByExternalId("ext-a")).willReturn(Optional.of(userA));
        given(participantRepository.existsByDmChannelIdAndUserIdAndLeftAtIsNull(10L, 1L)).willReturn(true);
        given(channelRepository.findById(10L)).willReturn(Optional.of(directChannel));
        given(messageRepository.findById(200L)).willReturn(Optional.of(newMessage));
        given(readStatusRepository.findByDmChannelIdAndUserId(10L, 1L)).willReturn(Optional.of(status));

        // when
        dmService.markRead("ext-a", 10L, 200L);

        // then
        assertThat(status.getLastReadMessage().getId()).isEqualTo(200L);
    }
}
