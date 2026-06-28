package kr.it.rudy.chat.server.domain;

import jakarta.persistence.*;
import kr.it.rudy.chat.common.domain.BaseEntity;
import kr.it.rudy.chat.user.domain.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "servers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Server extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 512)
    private String iconUrl;

    @Column(length = 16)
    private String inviteCode;

    @Column(nullable = false)
    private boolean isPublic;

    @Builder(access = AccessLevel.PRIVATE)
    private Server(User owner, String name, String description, boolean isPublic) {
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.isPublic = isPublic;
    }

    public static Server create(User owner, String name, String description, boolean isPublic) {
        return Server.builder()
                .owner(owner)
                .name(name)
                .description(description)
                .isPublic(isPublic)
                .build();
    }

    public void update(String name, String description, boolean isPublic) {
        this.name = name;
        this.description = description;
        this.isPublic = isPublic;
    }

    public boolean isOwnedBy(User user) {
        return this.owner.getId().equals(user.getId());
    }
}
