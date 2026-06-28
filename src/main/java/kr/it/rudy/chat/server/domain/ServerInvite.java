package kr.it.rudy.chat.server.domain;

import jakarta.persistence.*;
import kr.it.rudy.chat.user.domain.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "server_invites")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServerInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false, length = 16, unique = true)
    private String code;

    private Integer maxUses;

    @Column(nullable = false)
    private int uses;

    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private ServerInvite(Server server, User createdBy, String code, Integer maxUses, Instant expiresAt) {
        this.server = server;
        this.createdBy = createdBy;
        this.code = code;
        this.maxUses = maxUses;
        this.uses = 0;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public static ServerInvite create(Server server, User createdBy, String code, Integer maxUses, Instant expiresAt) {
        return ServerInvite.builder()
                .server(server)
                .createdBy(createdBy)
                .code(code)
                .maxUses(maxUses)
                .expiresAt(expiresAt)
                .build();
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isExhausted() {
        return maxUses != null && uses >= maxUses;
    }

    public void incrementUses() {
        this.uses++;
    }
}
