package kr.it.rudy.chat.server.domain;

import jakarta.persistence.*;
import kr.it.rudy.chat.user.domain.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "server_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServerMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 32)
    private String nickname;

    @Column(nullable = false)
    private Instant joinedAt;

    @ManyToMany
    @JoinTable(
            name = "server_member_roles",
            joinColumns = @JoinColumn(name = "server_member_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<ServerRole> roles = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private ServerMember(Server server, User user) {
        this.server = server;
        this.user = user;
        this.joinedAt = Instant.now();
    }

    public static ServerMember create(Server server, User user) {
        return ServerMember.builder()
                .server(server)
                .user(user)
                .build();
    }
}
