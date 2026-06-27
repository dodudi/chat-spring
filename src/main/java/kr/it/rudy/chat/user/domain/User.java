package kr.it.rudy.chat.user.domain;

import jakarta.persistence.*;
import kr.it.rudy.chat.common.domain.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private User(String externalId) {
        this.externalId = externalId;
        this.status = UserStatus.OFFLINE;
    }

    public static User create(String externalId) {
        return User.builder()
                .externalId(externalId)
                .build();
    }

    public void updateStatus(UserStatus status) {
        this.status = status;
    }
}
