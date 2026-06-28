package kr.it.rudy.chat.notification.domain;

import jakarta.persistence.*;
import kr.it.rudy.chat.channel.domain.Channel;
import kr.it.rudy.chat.server.domain.Server;
import kr.it.rudy.chat.user.domain.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "notification_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id")
    private Server server;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id")
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MuteLevel muteLevel;

    private Instant mutedUntil;

    @Builder(access = AccessLevel.PRIVATE)
    private NotificationSetting(User user, Server server, Channel channel, MuteLevel muteLevel, Instant mutedUntil) {
        this.user = user;
        this.server = server;
        this.channel = channel;
        this.muteLevel = muteLevel;
        this.mutedUntil = mutedUntil;
    }

    public static NotificationSetting create(User user, Server server, Channel channel, MuteLevel muteLevel, Instant mutedUntil) {
        return NotificationSetting.builder()
                .user(user)
                .server(server)
                .channel(channel)
                .muteLevel(muteLevel)
                .mutedUntil(mutedUntil)
                .build();
    }

    public void update(MuteLevel muteLevel, Instant mutedUntil) {
        this.muteLevel = muteLevel;
        this.mutedUntil = mutedUntil;
    }

    public boolean isOwnedBy(User user) {
        return this.user.getId().equals(user.getId());
    }
}
