package kr.it.rudy.chat.channel.domain;

import jakarta.persistence.*;
import kr.it.rudy.chat.server.domain.Server;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "channel_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private Instant createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private ChannelCategory(Server server, String name, int position) {
        this.server = server;
        this.name = name;
        this.position = position;
        this.createdAt = Instant.now();
    }

    public static ChannelCategory create(Server server, String name, int position) {
        return ChannelCategory.builder()
                .server(server)
                .name(name)
                .position(position)
                .build();
    }
}
