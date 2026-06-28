package kr.it.rudy.chat.message.domain;

import jakarta.persistence.*;
import kr.it.rudy.chat.user.domain.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "message_reactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 64)
    private String emoji;

    @Column(nullable = false)
    private Instant createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private MessageReaction(Message message, User user, String emoji) {
        this.message = message;
        this.user = user;
        this.emoji = emoji;
        this.createdAt = Instant.now();
    }

    public static MessageReaction create(Message message, User user, String emoji) {
        return MessageReaction.builder()
                .message(message)
                .user(user)
                .emoji(emoji)
                .build();
    }
}
