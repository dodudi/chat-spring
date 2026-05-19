# 도메인 규칙

## Entity 설계

```java
@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RoomType type;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 정적 팩토리 메서드로 생성
    public static ChatRoom create(RoomType type, String createdBy) {
        ChatRoom room = new ChatRoom();
        room.type = type;
        room.createdBy = createdBy;
        return room;
    }

    // 상태 변경은 의미 있는 메서드로
    public void updateName(String name) {
        this.name = name;
    }
}
```

- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` — 외부 직접 생성 금지
- `@Setter` / `@Data` 사용 금지
- 타임스탬프 필드 타입: `OffsetDateTime` (`LocalDateTime` 사용 금지 — 타임존 손실)
- `@CreationTimestamp` / `@UpdateTimestamp`로 Hibernate가 자동 관리

## DTO 설계

```java
// Request — record + Validation
public record CreateGroupRoomRequest(
        @NotBlank String name,
        @Size(min = 2) List<String> memberIds
) {}

// Response — 정적 팩토리 변환
public record RoomSummaryResponse(Long id, String name) {
    public static RoomSummaryResponse from(ChatRoom room) {
        return new RoomSummaryResponse(room.getId(), room.getName());
    }
}
```

- Entity ↔ DTO 변환은 `application` 레이어(Service)에서 수행한다.
- Entity를 Controller 반환값으로 직접 노출하지 않는다.

## Enum

DB 저장 시 `@Enumerated(EnumType.STRING)` 사용. PostgreSQL 네이티브 ENUM 타입 사용 금지.
