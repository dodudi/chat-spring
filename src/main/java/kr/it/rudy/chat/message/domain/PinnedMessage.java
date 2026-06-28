package kr.it.rudy.chat.message.domain;

import jakarta.persistence.*;
import kr.it.rudy.chat.channel.domain.Channel;
import kr.it.rudy.chat.user.domain.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "pinned_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PinnedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pinned_by", nullable = false)
    private User pinnedBy;

    @Column(nullable = false)
    private Instant pinnedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private PinnedMessage(Channel channel, Message message, User pinnedBy) {
        this.channel = channel;
        this.message = message;
        this.pinnedBy = pinnedBy;
        this.pinnedAt = Instant.now();
    }

    public static PinnedMessage create(Channel channel, Message message, User pinnedBy) {
        return PinnedMessage.builder()
                .channel(channel)
                .message(message)
                .pinnedBy(pinnedBy)
                .build();
    }
}
