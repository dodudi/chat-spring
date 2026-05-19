# 데이터베이스 규칙

## 네이밍

| 대상 | 규칙 | 예시 |
|------|------|------|
| 테이블 | 복수형 snake_case | `chat_rooms`, `messages` |
| 컬럼 | snake_case | `created_at`, `sender_id` |
| PK | `id` 고정 | `id` |
| FK | `{참조_테이블_단수}_id` | `room_id`, `sender_id` |
| 인덱스 | `idx_{테이블}_{컬럼}` | `idx_messages_room_id` |
| 유니크 인덱스 | `uidx_{테이블}_{컬럼}` | `uidx_members_room_user` |
| 제약 | `chk_{테이블}_{설명}` | `chk_rooms_type` |

## 기본 컬럼

모든 테이블에 포함한다.

```sql
id         BIGINT      PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

- PK: `GENERATED ALWAYS AS IDENTITY` 사용. `SERIAL` 금지.
- 타임스탬프: `TIMESTAMPTZ` 사용. `TIMESTAMP` 금지.

## 컬럼 타입

| 용도 | 타입 |
|------|------|
| PK / FK | `BIGINT` |
| JWT subject / user_id | `VARCHAR(255)` |
| 열거형 | `VARCHAR(50)` + CHECK 제약 |
| 긴 텍스트 | `TEXT` |
| 불리언 | `BOOLEAN NOT NULL DEFAULT false` |
| 타임스탬프 | `TIMESTAMPTZ` |
| soft delete | `deleted_at TIMESTAMPTZ DEFAULT NULL` |

PostgreSQL 네이티브 ENUM 타입 사용 금지 → `VARCHAR` + CHECK 제약으로 대체.

## Flyway 마이그레이션

```
src/main/resources/db/migration/
├── V1__create_chat_rooms.sql
├── V2__create_messages.sql
```

- 파일명: `V{버전}__{설명}.sql` (언더바 **두 개**)
- 버전은 정수 순번 사용
- **한 번 적용된 파일은 절대 수정하지 않는다** — 변경이 필요하면 새 버전으로 추가
- 마이그레이션 파일 변경 시 `docs/schema.dbml`을 반드시 함께 업데이트한다

## JPA Repository 쿼리

`@ManyToOne` 관계 탐색 파생 쿼리 대신 `@Query` JPQL을 사용한다.

```java
// ✅
@Query("SELECT m FROM ChatRoomMember m WHERE m.room.id = :roomId AND m.userId = :userId")
Optional<ChatRoomMember> findMember(@Param("roomId") Long roomId, @Param("userId") String userId);

// ❌
Optional<ChatRoomMember> findByRoom_IdAndUserId(Long roomId, String userId);
```

## Soft Delete

삭제 가능한 테이블은 `deleted_at TIMESTAMPTZ` 컬럼으로 soft delete를 구현한다.

```java
// Spring Boot 4.x / Hibernate 7.x
@SQLRestriction("deleted_at IS NULL")
public class Message { ... }
```
