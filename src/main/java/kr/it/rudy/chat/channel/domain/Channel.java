package kr.it.rudy.chat.channel.domain;

import jakarta.persistence.*;
import kr.it.rudy.chat.common.domain.BaseEntity;
import kr.it.rudy.chat.server.domain.Server;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "channels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Channel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ChannelCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelType type;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1024)
    private String description;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean isNsfw;

    @Column(nullable = false)
    private int slowmodeSeconds;

    @Builder(access = AccessLevel.PRIVATE)
    private Channel(Server server, ChannelCategory category, ChannelType type,
                    String name, String description, int position) {
        this.server = server;
        this.category = category;
        this.type = type;
        this.name = name;
        this.description = description;
        this.position = position;
        this.isNsfw = false;
        this.slowmodeSeconds = 0;
    }

    public static Channel create(Server server, ChannelCategory category, ChannelType type,
                                 String name, String description, int position) {
        return Channel.builder()
                .server(server)
                .category(category)
                .type(type)
                .name(name)
                .description(description)
                .position(position)
                .build();
    }

    public void update(String name, String description, boolean isNsfw, int slowmodeSeconds) {
        this.name = name;
        this.description = description;
        this.isNsfw = isNsfw;
        this.slowmodeSeconds = slowmodeSeconds;
    }
}
