package kr.it.rudy.chat.dm.application;

import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
import kr.it.rudy.chat.common.websocket.WebSocketDestination;
import kr.it.rudy.chat.common.websocket.WebSocketEvent;
import kr.it.rudy.chat.dm.domain.*;
import kr.it.rudy.chat.dm.dto.*;
import kr.it.rudy.chat.user.domain.User;
import kr.it.rudy.chat.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SimpleDmService implements DmService {

    private final DmChannelRepository channelRepository;
    private final DmChannelParticipantRepository participantRepository;
    private final DmMessageRepository messageRepository;
    private final DmReadStatusRepository readStatusRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public DmChannelResponse createDirectChannel(String externalId, Long targetUserId) {
        User requester = findUser(externalId);
        User target = findUserById(targetUserId);
        return participantRepository.findExistingChannel(requester.getId(), target.getId(), DmChannelType.DIRECT)
                .map(DmChannelResponse::from)
                .orElseGet(() -> {
                    DmChannel channel = channelRepository.save(DmChannel.createDirect());
                    participantRepository.save(DmChannelParticipant.create(channel, requester));
                    participantRepository.save(DmChannelParticipant.create(channel, target));
                    log.info("[DM_CREATE_DIRECT] requesterId={} targetId={} channelId={}",
                            requester.getId(), target.getId(), channel.getId());
                    return DmChannelResponse.from(channel);
                });
    }

    @Override
    @Transactional
    public DmChannelResponse createGroupChannel(String externalId, CreateGroupDmRequest request) {
        User creator = findUser(externalId);
        DmChannel channel = channelRepository.save(DmChannel.createGroup(request.name(), request.iconUrl()));
        participantRepository.save(DmChannelParticipant.create(channel, creator));
        for (Long participantId : request.participantIds()) {
            User participant = findUserById(participantId);
            participantRepository.save(DmChannelParticipant.create(channel, participant));
        }
        log.info("[DM_CREATE_GROUP] creatorId={} channelId={}", creator.getId(), channel.getId());
        return DmChannelResponse.from(channel);
    }

    @Override
    public List<DmChannelResponse> getMyChannels(String externalId) {
        User user = findUser(externalId);
        return participantRepository.findByUserIdAndLeftAtIsNull(user.getId())
                .stream().map(p -> DmChannelResponse.from(p.getDmChannel())).toList();
    }

    @Override
    @Transactional
    public void addParticipant(String externalId, Long channelId, Long userId) {
        DmChannel channel = findChannel(channelId);
        if (channel.isDirect()) {
            throw new AuthException(ErrorCode.DM_DIRECT_CANNOT_ADD_PARTICIPANT);
        }
        requireParticipant(channelId, findUser(externalId).getId());
        User newUser = findUserById(userId);
        participantRepository.findByDmChannelIdAndUserId(channelId, userId)
                .ifPresentOrElse(
                        p -> { if (p.hasLeft()) p.rejoin(); },
                        () -> participantRepository.save(DmChannelParticipant.create(channel, newUser))
                );
        log.info("[DM_ADD_PARTICIPANT] channelId={} userId={}", channelId, userId);
    }

    @Override
    @Transactional
    public void leaveChannel(String externalId, Long channelId) {
        User user = findUser(externalId);
        DmChannelParticipant participant = participantRepository
                .findByDmChannelIdAndUserIdAndLeftAtIsNull(channelId, user.getId())
                .orElseThrow(() -> new AuthException(ErrorCode.DM_NOT_PARTICIPANT));
        participant.leave();
        log.info("[DM_LEAVE] channelId={} userId={}", channelId, user.getId());
    }

    @Override
    @Transactional
    public DmMessageResponse sendMessage(String externalId, Long channelId, SendDmMessageRequest request) {
        User user = findUser(externalId);
        DmChannel channel = findChannel(channelId);
        requireParticipant(channelId, user.getId());
        DmMessage parentMessage = null;
        if (request.parentMessageId() != null) {
            parentMessage = findMessage(request.parentMessageId());
        }
        DmMessage message = messageRepository.save(DmMessage.create(channel, user, request.content(), parentMessage));
        log.info("[DM_MESSAGE_SEND] channelId={} senderId={} messageId={}", channelId, user.getId(), message.getId());
        messagingTemplate.convertAndSend(WebSocketDestination.dm(channelId),
                WebSocketEvent.of("MESSAGE_CREATED", DmMessageResponse.from(message)));
        return DmMessageResponse.from(message);
    }

    @Override
    public List<DmMessageResponse> findMessages(Long channelId, Long before, int limit) {
        if (!channelRepository.existsById(channelId)) {
            throw new AuthException(ErrorCode.DM_CHANNEL_NOT_FOUND);
        }
        PageRequest pageable = PageRequest.of(0, limit);
        List<DmMessage> messages = before != null
                ? messageRepository.findByDmChannelIdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(channelId, before, pageable)
                : messageRepository.findByDmChannelIdAndDeletedAtIsNullOrderByIdDesc(channelId, pageable);
        return messages.stream().map(DmMessageResponse::from).toList();
    }

    @Override
    @Transactional
    public DmMessageResponse editMessage(String externalId, Long messageId, EditDmMessageRequest request) {
        User user = findUser(externalId);
        DmMessage message = findMessage(messageId);
        if (!message.isSentBy(user)) {
            throw new AuthException(ErrorCode.DM_MESSAGE_EDIT_FORBIDDEN);
        }
        message.edit(request.content());
        log.info("[DM_MESSAGE_EDIT] messageId={} userId={}", messageId, user.getId());
        messagingTemplate.convertAndSend(WebSocketDestination.dm(message.getDmChannel().getId()),
                WebSocketEvent.of("MESSAGE_EDITED", DmMessageResponse.from(message)));
        return DmMessageResponse.from(message);
    }

    @Override
    @Transactional
    public void deleteMessage(String externalId, Long messageId) {
        User user = findUser(externalId);
        DmMessage message = findMessage(messageId);
        if (!message.isSentBy(user)) {
            throw new AuthException(ErrorCode.DM_MESSAGE_DELETE_FORBIDDEN);
        }
        long channelId = message.getDmChannel().getId();
        message.softDelete();
        log.info("[DM_MESSAGE_DELETE] messageId={} userId={}", messageId, user.getId());
        messagingTemplate.convertAndSend(WebSocketDestination.dm(channelId),
                WebSocketEvent.of("MESSAGE_DELETED", messageId));
    }

    @Override
    @Transactional
    public void markRead(String externalId, Long channelId, Long lastReadMessageId) {
        User user = findUser(externalId);
        requireParticipant(channelId, user.getId());
        DmChannel channel = findChannel(channelId);
        DmMessage message = findMessage(lastReadMessageId);
        readStatusRepository.findByDmChannelIdAndUserId(channelId, user.getId())
                .ifPresentOrElse(
                        status -> status.update(message),
                        () -> readStatusRepository.save(DmReadStatus.create(channel, user, message))
                );
        log.info("[DM_MARK_READ] channelId={} userId={} messageId={}", channelId, user.getId(), lastReadMessageId);
    }

    private User findUser(String externalId) {
        return userRepository.findByExternalId(externalId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
    }

    private DmChannel findChannel(Long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new AuthException(ErrorCode.DM_CHANNEL_NOT_FOUND));
    }

    private DmMessage findMessage(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new AuthException(ErrorCode.DM_MESSAGE_NOT_FOUND));
    }

    private void requireParticipant(Long channelId, Long userId) {
        if (!participantRepository.existsByDmChannelIdAndUserIdAndLeftAtIsNull(channelId, userId)) {
            throw new AuthException(ErrorCode.DM_NOT_PARTICIPANT);
        }
    }
}
