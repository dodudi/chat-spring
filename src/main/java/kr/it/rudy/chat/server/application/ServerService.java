package kr.it.rudy.chat.server.application;

import kr.it.rudy.chat.server.dto.*;

import java.util.List;

public interface ServerService {
    ServerResponse createServer(String externalId, CreateServerRequest request);
    ServerResponse findById(Long serverId);
    ServerResponse updateServer(String externalId, Long serverId, UpdateServerRequest request);
    void deleteServer(String externalId, Long serverId);
    ServerMemberResponse joinServer(String externalId, String inviteCode);
    void leaveServer(String externalId, Long serverId);
    List<ServerMemberResponse> findMembers(Long serverId);
    InviteResponse createInvite(String externalId, Long serverId, CreateInviteRequest request);
}
