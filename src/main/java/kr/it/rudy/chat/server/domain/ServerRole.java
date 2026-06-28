package kr.it.rudy.chat.server.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "server_roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServerRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 7)
    private String color;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private long permissions;

    @Column(nullable = false)
    private boolean isMentionable;

    @Column(nullable = false)
    private boolean isHoist;

    @Column(nullable = false)
    private Instant createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private ServerRole(Server server, String name, String color, int position) {
        this.server = server;
        this.name = name;
        this.color = color;
        this.position = position;
        this.permissions = 0L;
        this.isMentionable = true;
        this.isHoist = false;
        this.createdAt = Instant.now();
    }

    public static ServerRole create(Server server, String name, String color, int position) {
        return ServerRole.builder()
                .server(server)
                .name(name)
                .color(color)
                .position(position)
                .build();
    }
}
