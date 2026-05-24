package com.chat.websocket.pubsub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());

        if (channel.startsWith("pubsub:room:")) {
            String roomId = channel.substring("pubsub:room:".length());
            messagingTemplate.convertAndSend("/topic/rooms/" + roomId, body);
        } else if (channel.startsWith("pubsub:user:")) {
            String userId = channel.substring("pubsub:user:".length());
            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", body);
        }
    }
}
