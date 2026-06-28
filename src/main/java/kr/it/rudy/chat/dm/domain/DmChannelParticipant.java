package kr.it.rudy.chat.dm.domain;

import jakarta.persistence.*;
import kr.it.rudy.chat.user.domain.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "dm_channel_participants")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DmChannelParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dm_channel_id", nullable = false)
    private DmChannel dmChannel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreatedDate
    @Column(name = "joined_at", updatable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Builder(access = AccessLevel.PRIVATE)
    private DmChannelParticipant(DmChannel dmChannel, User user) {
        this.dmChannel = dmChannel;
        this.user = user;
    }

    public static DmChannelParticipant create(DmChannel dmChannel, User user) {
        return DmChannelParticipant.builder().dmChannel(dmChannel).user(user).build();
    }

    public void leave() {
        this.leftAt = Instant.now();
    }

    public void rejoin() {
        this.leftAt = null;
    }

    public boolean hasLeft() {
        return this.leftAt != null;
    }
}
