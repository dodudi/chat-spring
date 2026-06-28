package kr.it.rudy.chat.dm.application;

import kr.it.rudy.chat.dm.dto.*;

import java.util.List;

public interface DmService {
    DmChannelResponse createDirectChannel(String externalId, Long targetUserId);

    DmChannelResponse createGroupChannel(String externalId, CreateGroupDmRequest request);

    List<DmChannelResponse> getMyChannels(String externalId);

    void addParticipant(String externalId, Long channelId, Long userId);

    void leaveChannel(String externalId, Long channelId);

    DmMessageResponse sendMessage(String externalId, Long channelId, SendDmMessageRequest request);

    List<DmMessageResponse> findMessages(Long channelId, Long before, int limit);

    DmMessageResponse editMessage(String externalId, Long messageId, EditDmMessageRequest request);

    void deleteMessage(String externalId, Long messageId);

    void markRead(String externalId, Long channelId, Long lastReadMessageId);
}
