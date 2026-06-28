package kr.it.rudy.chat.server.application;

import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
import kr.it.rudy.chat.server.domain.*;
import kr.it.rudy.chat.server.dto.*;
import kr.it.rudy.chat.user.domain.User;
import kr.it.rudy.chat.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SimpleServerService implements ServerService {

    private final ServerRepository serverRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final ServerInviteRepository serverInviteRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ServerResponse createServer(String externalId, CreateServerRequest request) {
        User owner = findUser(externalId);
        Server server = serverRepository.save(
                Server.create(owner, request.name(), request.description(), request.isPublic())
        );
        serverMemberRepository.save(ServerMember.create(server, owner));
        log.info("[SERVER_CREATE] externalId={} serverId={}", externalId, server.getId());
        return ServerResponse.from(server);
    }

    @Override
    public ServerResponse findById(Long serverId) {
        return ServerResponse.from(findServer(serverId));
    }

    @Override
    @Transactional
    public ServerResponse updateServer(String externalId, Long serverId, UpdateServerRequest request) {
        User user = findUser(externalId);
        Server server = findServer(serverId);
        if (!server.isOwnedBy(user)) {
            throw new AuthException(ErrorCode.SERVER_FORBIDDEN);
        }
        server.update(request.name(), request.description(), request.isPublic());
        log.info("[SERVER_UPDATE] externalId={} serverId={}", externalId, serverId);
        return ServerResponse.from(server);
    }

    @Override
    @Transactional
    public void deleteServer(String externalId, Long serverId) {
        User user = findUser(externalId);
        Server server = findServer(serverId);
        if (!server.isOwnedBy(user)) {
            throw new AuthException(ErrorCode.SERVER_FORBIDDEN);
        }
        serverRepository.delete(server);
        log.info("[SERVER_DELETE] externalId={} serverId={}", externalId, serverId);
    }

    @Override
    @Transactional
    public ServerMemberResponse joinServer(String externalId, String inviteCode) {
        User user = findUser(externalId);
        ServerInvite invite = serverInviteRepository.findByCode(inviteCode)
                .orElseThrow(() -> new AuthException(ErrorCode.INVITE_NOT_FOUND));
        if (invite.isExpired()) {
            throw new AuthException(ErrorCode.INVITE_EXPIRED);
        }
        if (invite.isExhausted()) {
            throw new AuthException(ErrorCode.INVITE_EXHAUSTED);
        }
        if (serverMemberRepository.existsByServerIdAndUserId(invite.getServer().getId(), user.getId())) {
            throw new AuthException(ErrorCode.SERVER_ALREADY_JOINED);
        }
        invite.incrementUses();
        ServerMember member = serverMemberRepository.save(ServerMember.create(invite.getServer(), user));
        log.info("[SERVER_JOIN] externalId={} serverId={}", externalId, invite.getServer().getId());
        return ServerMemberResponse.from(member);
    }

    @Override
    @Transactional
    public void leaveServer(String externalId, Long serverId) {
        User user = findUser(externalId);
        Server server = findServer(serverId);
        if (server.isOwnedBy(user)) {
            throw new AuthException(ErrorCode.SERVER_OWNER_CANNOT_LEAVE);
        }
        ServerMember member = serverMemberRepository.findByServerIdAndUserId(serverId, user.getId())
                .orElseThrow(() -> new AuthException(ErrorCode.SERVER_MEMBER_NOT_FOUND));
        serverMemberRepository.delete(member);
        log.info("[SERVER_LEAVE] externalId={} serverId={}", externalId, serverId);
    }

    @Override
    public List<ServerMemberResponse> findMembers(Long serverId) {
        if (!serverRepository.existsById(serverId)) {
            throw new AuthException(ErrorCode.SERVER_NOT_FOUND);
        }
        return serverMemberRepository.findAllByServerId(serverId).stream()
                .map(ServerMemberResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public InviteResponse createInvite(String externalId, Long serverId, CreateInviteRequest request) {
        User user = findUser(externalId);
        Server server = findServer(serverId);
        if (!serverMemberRepository.existsByServerIdAndUserId(serverId, user.getId())) {
            throw new AuthException(ErrorCode.SERVER_MEMBER_NOT_FOUND);
        }
        Instant expiresAt = request.expiresInSeconds() != null
                ? Instant.now().plusSeconds(request.expiresInSeconds())
                : null;
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        ServerInvite invite = serverInviteRepository.save(
                ServerInvite.create(server, user, code, request.maxUses(), expiresAt)
        );
        log.info("[INVITE_CREATE] externalId={} serverId={} code={}", externalId, serverId, code);
        return InviteResponse.from(invite);
    }

    private User findUser(String externalId) {
        return userRepository.findByExternalId(externalId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
    }

    private Server findServer(Long serverId) {
        return serverRepository.findById(serverId)
                .orElseThrow(() -> new AuthException(ErrorCode.SERVER_NOT_FOUND));
    }
}
