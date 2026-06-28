package kr.it.rudy.chat.dm.domain;

import jakarta.persistence.*;
import kr.it.rudy.chat.user.domain.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "dm_read_status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DmReadStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dm_channel_id", nullable = false)
    private DmChannel dmChannel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id", nullable = false)
    private DmMessage lastReadMessage;

    @Column(nullable = false)
    private Instant lastReadAt;

    @Builder(access = AccessLevel.PRIVATE)
    private DmReadStatus(DmChannel dmChannel, User user, DmMessage lastReadMessage) {
        this.dmChannel = dmChannel;
        this.user = user;
        this.lastReadMessage = lastReadMessage;
        this.lastReadAt = Instant.now();
    }

    public static DmReadStatus create(DmChannel dmChannel, User user, DmMessage lastReadMessage) {
        return DmReadStatus.builder()
                .dmChannel(dmChannel)
                .user(user)
                .lastReadMessage(lastReadMessage)
                .build();
    }

    public void update(DmMessage lastReadMessage) {
        this.lastReadMessage = lastReadMessage;
        this.lastReadAt = Instant.now();
    }
}
