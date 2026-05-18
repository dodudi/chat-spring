package com.chat.websocket.redis;

import com.chat.websocket.dto.ChatBroadcastMessage;
import com.chat.websocket.dto.NotificationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageSubscriberTest {

    @Mock SimpMessagingTemplate messagingTemplate;

    private ChatMessageSubscriber subscriber;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        subscriber = new ChatMessageSubscriber(messagingTemplate, objectMapper);
    }

    @Test
    void onMessage_room채널이면_topic으로_브로드캐스트() throws Exception {
        // given
        ChatBroadcastMessage broadcast = new ChatBroadcastMessage(1L, 10L, "user-a", "안녕", null, null);
        byte[] body = objectMapper.writeValueAsBytes(broadcast);
        Message redisMessage = stubMessage("pubsub:room:10", body);

        // when
        subscriber.onMessage(redisMessage, null);

        // then
        then(messagingTemplate).should().convertAndSend(eq("/topic/rooms/10"), any(ChatBroadcastMessage.class));
    }

    @Test
    void onMessage_user채널이면_유저_queue로_전송() throws Exception {
        // given
        NotificationMessage notification = new NotificationMessage("ROOM_INVITED", 10L, "팀방", "user-b");
        byte[] body = objectMapper.writeValueAsBytes(notification);
        Message redisMessage = stubMessage("pubsub:user:user-a", body);

        // when
        subscriber.onMessage(redisMessage, null);

        // then
        then(messagingTemplate).should()
                .convertAndSendToUser(eq("user-a"), eq("/queue/notifications"), any(NotificationMessage.class));
    }

    @Test
    void onMessage_알_수_없는_채널이면_무시() throws Exception {
        // given
        Message redisMessage = stubMessage("unknown:channel", "{}".getBytes());

        // when
        subscriber.onMessage(redisMessage, null);

        // then
        then(messagingTemplate).shouldHaveNoInteractions();
    }

    private Message stubMessage(String channel, byte[] body) {
        return new Message() {
            @Override public byte[] getBody() { return body; }
            @Override public byte[] getChannel() { return channel.getBytes(); }
        };
    }
}
