package kr.it.rudy.chat.message.application;

import kr.it.rudy.chat.message.dto.*;

import java.util.List;

public interface MessageService {
    MessageResponse sendMessage(String externalId, Long channelId, SendMessageRequest request);
    List<MessageResponse> findMessages(Long channelId, Long before, int limit);
    MessageResponse editMessage(String externalId, Long messageId, EditMessageRequest request);
    void deleteMessage(String externalId, Long messageId);
    ReactionResponse addReaction(String externalId, Long messageId, String emoji);
    void removeReaction(String externalId, Long messageId, String emoji);
    PinnedMessageResponse pinMessage(String externalId, Long channelId, Long messageId);
    void unpinMessage(String externalId, Long channelId, Long messageId);
    List<PinnedMessageResponse> findPinnedMessages(Long channelId);
}
