package kr.it.rudy.chat.message.application;

import kr.it.rudy.chat.channel.domain.Channel;
import kr.it.rudy.chat.channel.domain.ChannelRepository;
import kr.it.rudy.chat.channel.domain.ChannelType;
import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
import kr.it.rudy.chat.message.domain.*;
import kr.it.rudy.chat.message.dto.*;
import kr.it.rudy.chat.server.domain.Server;
import kr.it.rudy.chat.server.domain.ServerMemberRepository;
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
class SimpleMessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private MessageReactionRepository reactionRepository;
    @Mock private PinnedMessageRepository pinnedMessageRepository;
    @Mock private ChannelRepository channelRepository;
    @Mock private ServerMemberRepository serverMemberRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private SimpleMessageService messageService;

    private User sender;
    private User other;
    private Server server;
    private Channel channel;
    private Message message;

    @BeforeEach
    void setUp() {
        sender = User.create("ext-sender");
        ReflectionTestUtils.setField(sender, "id", 1L);

        other = User.create("ext-other");
        ReflectionTestUtils.setField(other, "id", 2L);

        server = Server.create(sender, "테스트 서버", null, false);
        ReflectionTestUtils.setField(server, "id", 10L);

        channel = Channel.create(server, null, ChannelType.TEXT, "general", null, 0);
        ReflectionTestUtils.setField(channel, "id", 20L);

        message = Message.create(channel, sender, "안녕하세요", MessageType.DEFAULT, null);
        ReflectionTestUtils.setField(message, "id", 100L);
    }

    @Test
    void sendMessage_서버_멤버가_메시지_전송시_MessageResponse_반환() {
        // given
        given(userRepository.findByExternalId("ext-sender")).willReturn(Optional.of(sender));
        given(channelRepository.findById(20L)).willReturn(Optional.of(channel));
        given(serverMemberRepository.existsByServerIdAndUserId(10L, 1L)).willReturn(true);
        given(messageRepository.save(any())).willReturn(message);

        // when
        MessageResponse response = messageService.sendMessage("ext-sender", 20L,
                new SendMessageRequest("안녕하세요", MessageType.DEFAULT, null));

        // then
        assertThat(response.content()).isEqualTo("안녕하세요");
        assertThat(response.type()).isEqualTo(MessageType.DEFAULT);
        assertThat(response.senderId()).isEqualTo(1L);
    }

    @Test
    void sendMessage_서버_멤버가_아닌_경우_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-other")).willReturn(Optional.of(other));
        given(channelRepository.findById(20L)).willReturn(Optional.of(channel));
        given(serverMemberRepository.existsByServerIdAndUserId(10L, 2L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> messageService.sendMessage("ext-other", 20L,
                new SendMessageRequest("안녕하세요", MessageType.DEFAULT, null)))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.CHANNEL_FORBIDDEN.getMessage());
    }

    @Test
    void sendMessage_답장_메시지_전송시_parentMessageId_포함된_MessageResponse_반환() {
        // given
        Message reply = Message.create(channel, sender, "답장입니다", MessageType.REPLY, message);
        ReflectionTestUtils.setField(reply, "id", 101L);

        given(userRepository.findByExternalId("ext-sender")).willReturn(Optional.of(sender));
        given(channelRepository.findById(20L)).willReturn(Optional.of(channel));
        given(serverMemberRepository.existsByServerIdAndUserId(10L, 1L)).willReturn(true);
        given(messageRepository.findById(100L)).willReturn(Optional.of(message));
        given(messageRepository.save(any())).willReturn(reply);

        // when
        MessageResponse response = messageService.sendMessage("ext-sender", 20L,
                new SendMessageRequest("답장입니다", MessageType.REPLY, 100L));

        // then
        assertThat(response.type()).isEqualTo(MessageType.REPLY);
        assertThat(response.parentMessageId()).isEqualTo(100L);
    }

    @Test
    void findMessages_커서_없이_최신_메시지_목록_반환() {
        // given
        given(channelRepository.existsById(20L)).willReturn(true);
        given(messageRepository.findByChannelIdAndDeletedAtIsNullOrderByIdDesc(any(Long.class), any(Pageable.class)))
                .willReturn(List.of(message));

        // when
        List<MessageResponse> responses = messageService.findMessages(20L, null, 50);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).content()).isEqualTo("안녕하세요");
    }

    @Test
    void findMessages_커서_지정시_해당_id_이전_메시지_반환() {
        // given
        given(channelRepository.existsById(20L)).willReturn(true);
        given(messageRepository.findByChannelIdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(any(Long.class), any(Long.class), any(Pageable.class)))
                .willReturn(List.of(message));

        // when
        List<MessageResponse> responses = messageService.findMessages(20L, 200L, 50);

        // then
        assertThat(responses).hasSize(1);
    }

    @Test
    void findMessages_존재하지_않는_채널_조회시_AuthException_발생() {
        // given
        given(channelRepository.existsById(999L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> messageService.findMessages(999L, null, 50))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.CHANNEL_NOT_FOUND.getMessage());
    }

    @Test
    void editMessage_발신자가_수정시_수정된_MessageResponse_반환() {
        // given
        given(userRepository.findByExternalId("ext-sender")).willReturn(Optional.of(sender));
        given(messageRepository.findById(100L)).willReturn(Optional.of(message));

        // when
        MessageResponse response = messageService.editMessage("ext-sender", 100L,
                new EditMessageRequest("수정된 내용"));

        // then
        assertThat(response.content()).isEqualTo("수정된 내용");
        assertThat(response.isEdited()).isTrue();
    }

    @Test
    void editMessage_발신자가_아닌_경우_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-other")).willReturn(Optional.of(other));
        given(messageRepository.findById(100L)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> messageService.editMessage("ext-other", 100L,
                new EditMessageRequest("수정된 내용")))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.MESSAGE_EDIT_FORBIDDEN.getMessage());
    }

    @Test
    void deleteMessage_발신자가_삭제시_소프트_삭제() {
        // given
        given(userRepository.findByExternalId("ext-sender")).willReturn(Optional.of(sender));
        given(messageRepository.findById(100L)).willReturn(Optional.of(message));

        // when
        messageService.deleteMessage("ext-sender", 100L);

        // then
        assertThat(message.isDeleted()).isTrue();
        assertThat(message.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteMessage_발신자가_아닌_경우_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-other")).willReturn(Optional.of(other));
        given(messageRepository.findById(100L)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> messageService.deleteMessage("ext-other", 100L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.MESSAGE_DELETE_FORBIDDEN.getMessage());
    }

    @Test
    void addReaction_정상_추가시_ReactionResponse_반환() {
        // given
        MessageReaction reaction = MessageReaction.create(message, sender, "👍");
        ReflectionTestUtils.setField(reaction, "id", 1L);

        given(userRepository.findByExternalId("ext-sender")).willReturn(Optional.of(sender));
        given(messageRepository.findById(100L)).willReturn(Optional.of(message));
        given(reactionRepository.existsByMessageIdAndUserIdAndEmoji(100L, 1L, "👍")).willReturn(false);
        given(reactionRepository.save(any())).willReturn(reaction);

        // when
        ReactionResponse response = messageService.addReaction("ext-sender", 100L, "👍");

        // then
        assertThat(response.emoji()).isEqualTo("👍");
        assertThat(response.userId()).isEqualTo(1L);
    }

    @Test
    void addReaction_이미_추가한_반응이면_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-sender")).willReturn(Optional.of(sender));
        given(messageRepository.findById(100L)).willReturn(Optional.of(message));
        given(reactionRepository.existsByMessageIdAndUserIdAndEmoji(100L, 1L, "👍")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> messageService.addReaction("ext-sender", 100L, "👍"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.REACTION_ALREADY_EXISTS.getMessage());
    }

    @Test
    void removeReaction_정상_제거시_삭제() {
        // given
        MessageReaction reaction = MessageReaction.create(message, sender, "👍");

        given(userRepository.findByExternalId("ext-sender")).willReturn(Optional.of(sender));
        given(reactionRepository.findByMessageIdAndUserIdAndEmoji(100L, 1L, "👍")).willReturn(Optional.of(reaction));

        // when
        messageService.removeReaction("ext-sender", 100L, "👍");

        // then
        then(reactionRepository).should().delete(reaction);
    }

    @Test
    void removeReaction_존재하지_않는_반응이면_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-sender")).willReturn(Optional.of(sender));
        given(reactionRepository.findByMessageIdAndUserIdAndEmoji(100L, 1L, "👍")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> messageService.removeReaction("ext-sender", 100L, "👍"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.REACTION_NOT_FOUND.getMessage());
    }

    @Test
    void pinMessage_서버_멤버가_고정시_PinnedMessageResponse_반환() {
        // given
        PinnedMessage pin = PinnedMessage.create(channel, message, sender);
        ReflectionTestUtils.setField(pin, "id", 1L);

        given(userRepository.findByExternalId("ext-sender")).willReturn(Optional.of(sender));
        given(channelRepository.findById(20L)).willReturn(Optional.of(channel));
        given(serverMemberRepository.existsByServerIdAndUserId(10L, 1L)).willReturn(true);
        given(messageRepository.findById(100L)).willReturn(Optional.of(message));
        given(pinnedMessageRepository.existsByChannelIdAndMessageId(20L, 100L)).willReturn(false);
        given(pinnedMessageRepository.save(any())).willReturn(pin);

        // when
        PinnedMessageResponse response = messageService.pinMessage("ext-sender", 20L, 100L);

        // then
        assertThat(response.channelId()).isEqualTo(20L);
        assertThat(response.messageId()).isEqualTo(100L);
    }

    @Test
    void pinMessage_이미_고정된_메시지면_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-sender")).willReturn(Optional.of(sender));
        given(channelRepository.findById(20L)).willReturn(Optional.of(channel));
        given(serverMemberRepository.existsByServerIdAndUserId(10L, 1L)).willReturn(true);
        given(messageRepository.findById(100L)).willReturn(Optional.of(message));
        given(pinnedMessageRepository.existsByChannelIdAndMessageId(20L, 100L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> messageService.pinMessage("ext-sender", 20L, 100L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.PIN_ALREADY_EXISTS.getMessage());
    }

    @Test
    void unpinMessage_정상_해제시_삭제() {
        // given
        PinnedMessage pin = PinnedMessage.create(channel, message, sender);

        given(userRepository.findByExternalId("ext-sender")).willReturn(Optional.of(sender));
        given(channelRepository.findById(20L)).willReturn(Optional.of(channel));
        given(serverMemberRepository.existsByServerIdAndUserId(10L, 1L)).willReturn(true);
        given(pinnedMessageRepository.findByChannelIdAndMessageId(20L, 100L)).willReturn(Optional.of(pin));

        // when
        messageService.unpinMessage("ext-sender", 20L, 100L);

        // then
        then(pinnedMessageRepository).should().delete(pin);
    }

    @Test
    void findPinnedMessages_고정_메시지_목록_반환() {
        // given
        PinnedMessage pin = PinnedMessage.create(channel, message, sender);
        ReflectionTestUtils.setField(pin, "id", 1L);

        given(channelRepository.existsById(20L)).willReturn(true);
        given(pinnedMessageRepository.findAllByChannelIdOrderByPinnedAtDesc(20L)).willReturn(List.of(pin));

        // when
        List<PinnedMessageResponse> responses = messageService.findPinnedMessages(20L);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).messageId()).isEqualTo(100L);
    }
}
