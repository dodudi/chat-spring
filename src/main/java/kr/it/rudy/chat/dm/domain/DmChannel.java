package kr.it.rudy.chat.dm.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "dm_channels")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DmChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DmChannelType type;

    private String name;

    private String iconUrl;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private DmChannel(DmChannelType type, String name, String iconUrl) {
        this.type = type;
        this.name = name;
        this.iconUrl = iconUrl;
    }

    public static DmChannel createDirect() {
        return DmChannel.builder().type(DmChannelType.DIRECT).build();
    }

    public static DmChannel createGroup(String name, String iconUrl) {
        return DmChannel.builder().type(DmChannelType.GROUP).name(name).iconUrl(iconUrl).build();
    }

    public boolean isDirect() {
        return this.type == DmChannelType.DIRECT;
    }
}
