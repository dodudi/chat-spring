package kr.it.rudy.chat.friend.domain;

import jakarta.persistence.*;
import kr.it.rudy.chat.common.domain.BaseEntity;
import kr.it.rudy.chat.user.domain.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "friend_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendRequestStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private FriendRequest(User requester, User receiver) {
        this.requester = requester;
        this.receiver = receiver;
        this.status = FriendRequestStatus.PENDING;
    }

    public static FriendRequest create(User requester, User receiver) {
        return FriendRequest.builder()
                .requester(requester)
                .receiver(receiver)
                .build();
    }

    public void accept() {
        this.status = FriendRequestStatus.ACCEPTED;
    }

    public void reject() {
        this.status = FriendRequestStatus.REJECTED;
    }

    public void cancel() {
        this.status = FriendRequestStatus.CANCELLED;
    }

    public boolean isReceivedBy(User user) {
        return this.receiver.getId().equals(user.getId());
    }

    public boolean isSentBy(User user) {
        return this.requester.getId().equals(user.getId());
    }
}
