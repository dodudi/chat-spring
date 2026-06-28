package kr.it.rudy.chat.notification.domain;

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
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String referenceType;

    @Column(nullable = false)
    private Long referenceId;

    @Column(nullable = false)
    private boolean isRead;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Notification(User user, NotificationType type, String referenceType, Long referenceId) {
        this.user = user;
        this.type = type;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.isRead = false;
    }

    public static Notification create(User user, NotificationType type, String referenceType, Long referenceId) {
        return Notification.builder()
                .user(user)
                .type(type)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();
    }

    public void markRead() {
        this.isRead = true;
    }

    public boolean isOwnedBy(User user) {
        return this.user.getId().equals(user.getId());
    }
}
