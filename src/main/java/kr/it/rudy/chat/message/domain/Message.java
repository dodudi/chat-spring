package kr.it.rudy.chat.message.domain;

import jakarta.persistence.*;
import kr.it.rudy.chat.channel.domain.Channel;
import kr.it.rudy.chat.common.domain.BaseEntity;
import kr.it.rudy.chat.user.domain.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_message_id")
    private Message parentMessage;

    @Column(nullable = false)
    private boolean isEdited;

    private Instant deletedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Message(Channel channel, User sender, String content, MessageType type, Message parentMessage) {
        this.channel = channel;
        this.sender = sender;
        this.content = content;
        this.type = type != null ? type : MessageType.DEFAULT;
        this.parentMessage = parentMessage;
        this.isEdited = false;
    }

    public static Message create(Channel channel, User sender, String content, MessageType type, Message parentMessage) {
        return Message.builder()
                .channel(channel)
                .sender(sender)
                .content(content)
                .type(type)
                .parentMessage(parentMessage)
                .build();
    }

    public void edit(String content) {
        this.content = content;
        this.isEdited = true;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public boolean isSentBy(User user) {
        return this.sender.getId().equals(user.getId());
    }
}
