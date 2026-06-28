package kr.it.rudy.chat.message.application;

import kr.it.rudy.chat.channel.domain.Channel;
import kr.it.rudy.chat.channel.domain.ChannelRepository;
import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
import kr.it.rudy.chat.message.domain.*;
import kr.it.rudy.chat.message.dto.*;
import kr.it.rudy.chat.server.domain.ServerMemberRepository;
import kr.it.rudy.chat.user.domain.User;
import kr.it.rudy.chat.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SimpleMessageService implements MessageService {

    private final MessageRepository messageRepository;
    private final MessageReactionRepository reactionRepository;
    private final PinnedMessageRepository pinnedMessageRepository;
    private final ChannelRepository channelRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public MessageResponse sendMessage(String externalId, Long channelId, SendMessageRequest request) {
        User user = findUser(externalId);
        Channel channel = findChannel(channelId);
        requireMember(channel.getServer().getId(), user.getId());
        Message parentMessage = null;
        if (request.type() == MessageType.REPLY && request.parentMessageId() != null) {
            parentMessage = findMessage(request.parentMessageId());
        }
        Message message = messageRepository.save(
                Message.create(channel, user, request.content(), request.type(), parentMessage)
        );
        log.info("[MESSAGE_SEND] externalId={} channelId={} messageId={}", externalId, channelId, message.getId());
        return MessageResponse.from(message);
    }

    @Override
    public List<MessageResponse> findMessages(Long channelId, Long before, int limit) {
        if (!channelRepository.existsById(channelId)) {
            throw new AuthException(ErrorCode.CHANNEL_NOT_FOUND);
        }
        PageRequest pageable = PageRequest.of(0, limit);
        List<Message> messages = before != null
                ? messageRepository.findByChannelIdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(channelId, before, pageable)
                : messageRepository.findByChannelIdAndDeletedAtIsNullOrderByIdDesc(channelId, pageable);
        return messages.stream().map(MessageResponse::from).toList();
    }

    @Override
    @Transactional
    public MessageResponse editMessage(String externalId, Long messageId, EditMessageRequest request) {
        User user = findUser(externalId);
        Message message = findMessage(messageId);
        if (!message.isSentBy(user)) {
            throw new AuthException(ErrorCode.MESSAGE_EDIT_FORBIDDEN);
        }
        message.edit(request.content());
        log.info("[MESSAGE_EDIT] externalId={} messageId={}", externalId, messageId);
        return MessageResponse.from(message);
    }

    @Override
    @Transactional
    public void deleteMessage(String externalId, Long messageId) {
        User user = findUser(externalId);
        Message message = findMessage(messageId);
        if (!message.isSentBy(user)) {
            throw new AuthException(ErrorCode.MESSAGE_DELETE_FORBIDDEN);
        }
        message.softDelete();
        log.info("[MESSAGE_DELETE] externalId={} messageId={}", externalId, messageId);
    }

    @Override
    @Transactional
    public ReactionResponse addReaction(String externalId, Long messageId, String emoji) {
        User user = findUser(externalId);
        Message message = findMessage(messageId);
        if (reactionRepository.existsByMessageIdAndUserIdAndEmoji(messageId, user.getId(), emoji)) {
            throw new AuthException(ErrorCode.REACTION_ALREADY_EXISTS);
        }
        MessageReaction reaction = reactionRepository.save(MessageReaction.create(message, user, emoji));
        log.info("[REACTION_ADD] externalId={} messageId={} emoji={}", externalId, messageId, emoji);
        return ReactionResponse.from(reaction);
    }

    @Override
    @Transactional
    public void removeReaction(String externalId, Long messageId, String emoji) {
        User user = findUser(externalId);
        MessageReaction reaction = reactionRepository
                .findByMessageIdAndUserIdAndEmoji(messageId, user.getId(), emoji)
                .orElseThrow(() -> new AuthException(ErrorCode.REACTION_NOT_FOUND));
        reactionRepository.delete(reaction);
        log.info("[REACTION_REMOVE] externalId={} messageId={} emoji={}", externalId, messageId, emoji);
    }

    @Override
    @Transactional
    public PinnedMessageResponse pinMessage(String externalId, Long channelId, Long messageId) {
        User user = findUser(externalId);
        Channel channel = findChannel(channelId);
        requireMember(channel.getServer().getId(), user.getId());
        Message message = findMessage(messageId);
        if (pinnedMessageRepository.existsByChannelIdAndMessageId(channelId, messageId)) {
            throw new AuthException(ErrorCode.PIN_ALREADY_EXISTS);
        }
        PinnedMessage pin = pinnedMessageRepository.save(PinnedMessage.create(channel, message, user));
        log.info("[MESSAGE_PIN] externalId={} channelId={} messageId={}", externalId, channelId, messageId);
        return PinnedMessageResponse.from(pin);
    }

    @Override
    @Transactional
    public void unpinMessage(String externalId, Long channelId, Long messageId) {
        User user = findUser(externalId);
        Channel channel = findChannel(channelId);
        requireMember(channel.getServer().getId(), user.getId());
        PinnedMessage pin = pinnedMessageRepository.findByChannelIdAndMessageId(channelId, messageId)
                .orElseThrow(() -> new AuthException(ErrorCode.PIN_NOT_FOUND));
        pinnedMessageRepository.delete(pin);
        log.info("[MESSAGE_UNPIN] externalId={} channelId={} messageId={}", externalId, channelId, messageId);
    }

    @Override
    public List<PinnedMessageResponse> findPinnedMessages(Long channelId) {
        if (!channelRepository.existsById(channelId)) {
            throw new AuthException(ErrorCode.CHANNEL_NOT_FOUND);
        }
        return pinnedMessageRepository.findAllByChannelIdOrderByPinnedAtDesc(channelId).stream()
                .map(PinnedMessageResponse::from)
                .toList();
    }

    private User findUser(String externalId) {
        return userRepository.findByExternalId(externalId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
    }

    private Channel findChannel(Long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new AuthException(ErrorCode.CHANNEL_NOT_FOUND));
    }

    private Message findMessage(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new AuthException(ErrorCode.MESSAGE_NOT_FOUND));
    }

    private void requireMember(Long serverId, Long userId) {
        if (!serverMemberRepository.existsByServerIdAndUserId(serverId, userId)) {
            throw new AuthException(ErrorCode.CHANNEL_FORBIDDEN);
        }
    }
}
