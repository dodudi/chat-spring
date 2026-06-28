package kr.it.rudy.chat.channel.application;

import kr.it.rudy.chat.channel.domain.Channel;
import kr.it.rudy.chat.channel.domain.ChannelCategory;
import kr.it.rudy.chat.channel.domain.ChannelCategoryRepository;
import kr.it.rudy.chat.channel.domain.ChannelRepository;
import kr.it.rudy.chat.channel.dto.*;
import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
import kr.it.rudy.chat.server.domain.Server;
import kr.it.rudy.chat.server.domain.ServerMemberRepository;
import kr.it.rudy.chat.server.domain.ServerRepository;
import kr.it.rudy.chat.user.domain.User;
import kr.it.rudy.chat.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SimpleChannelService implements ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelCategoryRepository channelCategoryRepository;
    private final ServerRepository serverRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CategoryResponse createCategory(String externalId, Long serverId, CreateCategoryRequest request) {
        User user = findUser(externalId);
        Server server = findServer(serverId);
        requireMember(serverId, user.getId());
        ChannelCategory category = channelCategoryRepository.save(
                ChannelCategory.create(server, request.name(), request.position())
        );
        log.info("[CATEGORY_CREATE] externalId={} serverId={} categoryId={}", externalId, serverId, category.getId());
        return CategoryResponse.from(category);
    }

    @Override
    @Transactional
    public ChannelResponse createChannel(String externalId, Long serverId, CreateChannelRequest request) {
        User user = findUser(externalId);
        Server server = findServer(serverId);
        requireMember(serverId, user.getId());
        ChannelCategory category = null;
        if (request.categoryId() != null) {
            category = channelCategoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new AuthException(ErrorCode.CHANNEL_CATEGORY_NOT_FOUND));
        }
        Channel channel = channelRepository.save(
                Channel.create(server, category, request.type(), request.name(), request.description(), request.position())
        );
        log.info("[CHANNEL_CREATE] externalId={} serverId={} channelId={}", externalId, serverId, channel.getId());
        return ChannelResponse.from(channel);
    }

    @Override
    public List<ChannelResponse> findChannelsByServer(Long serverId) {
        if (!serverRepository.existsById(serverId)) {
            throw new AuthException(ErrorCode.SERVER_NOT_FOUND);
        }
        return channelRepository.findAllByServerIdOrderByPosition(serverId).stream()
                .map(ChannelResponse::from)
                .toList();
    }

    @Override
    public ChannelResponse findById(Long channelId) {
        return ChannelResponse.from(findChannel(channelId));
    }

    @Override
    @Transactional
    public ChannelResponse updateChannel(String externalId, Long channelId, UpdateChannelRequest request) {
        User user = findUser(externalId);
        Channel channel = findChannel(channelId);
        requireMember(channel.getServer().getId(), user.getId());
        channel.update(request.name(), request.description(), request.isNsfw(), request.slowmodeSeconds());
        log.info("[CHANNEL_UPDATE] externalId={} channelId={}", externalId, channelId);
        return ChannelResponse.from(channel);
    }

    @Override
    @Transactional
    public void deleteChannel(String externalId, Long channelId) {
        User user = findUser(externalId);
        Channel channel = findChannel(channelId);
        requireMember(channel.getServer().getId(), user.getId());
        channelRepository.delete(channel);
        log.info("[CHANNEL_DELETE] externalId={} channelId={}", externalId, channelId);
    }

    private User findUser(String externalId) {
        return userRepository.findByExternalId(externalId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
    }

    private Server findServer(Long serverId) {
        return serverRepository.findById(serverId)
                .orElseThrow(() -> new AuthException(ErrorCode.SERVER_NOT_FOUND));
    }

    private Channel findChannel(Long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new AuthException(ErrorCode.CHANNEL_NOT_FOUND));
    }

    private void requireMember(Long serverId, Long userId) {
        if (!serverMemberRepository.existsByServerIdAndUserId(serverId, userId)) {
            throw new AuthException(ErrorCode.CHANNEL_FORBIDDEN);
        }
    }
}
