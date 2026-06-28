package kr.it.rudy.chat.dm.domain;

import jakarta.persistence.*;
import kr.it.rudy.chat.common.domain.BaseEntity;
import kr.it.rudy.chat.user.domain.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "dm_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DmMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dm_channel_id", nullable = false)
    private DmChannel dmChannel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_message_id")
    private DmMessage parentMessage;

    @Column(nullable = false)
    private boolean isEdited;

    private Instant deletedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private DmMessage(DmChannel dmChannel, User sender, String content, DmMessage parentMessage) {
        this.dmChannel = dmChannel;
        this.sender = sender;
        this.content = content;
        this.parentMessage = parentMessage;
        this.isEdited = false;
    }

    public static DmMessage create(DmChannel dmChannel, User sender, String content, DmMessage parentMessage) {
        return DmMessage.builder()
                .dmChannel(dmChannel)
                .sender(sender)
                .content(content)
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
