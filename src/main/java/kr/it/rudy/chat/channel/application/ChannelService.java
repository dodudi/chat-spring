package kr.it.rudy.chat.channel.application;

import kr.it.rudy.chat.channel.dto.*;

import java.util.List;

public interface ChannelService {
    CategoryResponse createCategory(String externalId, Long serverId, CreateCategoryRequest request);

    ChannelResponse createChannel(String externalId, Long serverId, CreateChannelRequest request);

    List<ChannelResponse> findChannelsByServer(Long serverId);

    ChannelResponse findById(Long channelId);

    ChannelResponse updateChannel(String externalId, Long channelId, UpdateChannelRequest request);

    void deleteChannel(String externalId, Long channelId);
}
