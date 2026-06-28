package kr.it.rudy.chat.friend.domain;

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
@Table(name = "friendships")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id", nullable = false)
    private User friend;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Friendship(User user, User friend) {
        this.user = user;
        this.friend = friend;
    }

    /** user_id < friend_id 제약: 항상 id가 작은 쪽을 user로 저장 */
    public static Friendship create(User userA, User userB) {
        if (userA.getId() < userB.getId()) {
            return Friendship.builder().user(userA).friend(userB).build();
        }
        return Friendship.builder().user(userB).friend(userA).build();
    }
}
