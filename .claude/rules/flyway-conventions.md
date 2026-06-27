# Flyway 규칙

이 파일은 DB 마이그레이션 파일 작성 시 항상 따라야 할 규칙을 정의한다.

---

## 파일 위치

마이그레이션 파일은 `src/main/resources/db/migration/`에 위치한다.

```
src/main/resources/db/migration/
├── V1__create_enum_types.sql
├── V2__create_user_tables.sql
├── V2.1__users_add_bio.sql
└── V{도메인버전}.{서브버전}__{설명}.sql
```

---

## 버전 체계 — 도메인별 소수점 서브버전

각 도메인은 고정된 정수 버전을 소유한다. 해당 도메인 테이블 변경은 그 번호의 서브버전으로 추가한다.

| 정수 버전 | 도메인 | 초기 파일 |
|---------|--------|---------|
| V1 | ENUM 타입 | `V1__create_enum_types.sql` |
| V2 | User | `V2__create_user_tables.sql` |
| V3 | Server | `V3__create_server_tables.sql` |
| V4 | Channel | `V4__create_channel_tables.sql` |
| V5 | Message | `V5__create_message_tables.sql` |
| V6 | Friend | `V6__create_friend_tables.sql` |
| V7 | DM | `V7__create_dm_tables.sql` |
| V8 | Notification | `V8__create_notification_tables.sql` |

서브버전은 `1`부터 순차 증가한다. 새 도메인은 `V9`부터 할당한다.

```
// ✅ 올바른 예 — users 테이블에 bio 컬럼 추가
V2.1__users_add_bio.sql

// ✅ 올바른 예 — servers 테이블에 region 컬럼 추가
V3.1__servers_add_region.sql

// ✅ 올바른 예 — 같은 도메인의 두 번째 변경
V2.2__users_add_username.sql

// ❌ 잘못된 예 — 도메인 불일치 (users 변경인데 V5 사용)
V5.1__users_add_bio.sql

// ❌ 잘못된 예 — 0 패딩
V02.1__users_add_bio.sql
```

---

## 파일명 규칙

```
V{도메인버전}.{서브버전}__{설명}.sql
```

| 구성 요소 | 규칙 |
|----------|------|
| 도메인버전 | 위 도메인 테이블의 정수 버전 |
| 서브버전 | 1부터 순차 증가 (0 패딩 금지) |
| 구분자 | 언더스코어 두 개 (`__`) |
| 설명 | 스네이크 케이스, `{테이블명}_{동사}` 형태 |

설명 네이밍 패턴:

| 작업 | 패턴 | 예시 |
|------|------|------|
| 컬럼 추가 | `{테이블}_add_{컬럼}` | `users_add_bio` |
| 컬럼 삭제 | `{테이블}_drop_{컬럼}` | `users_drop_legacy_field` |
| 컬럼 변경 | `{테이블}_alter_{컬럼}` | `users_alter_external_id_length` |
| 인덱스 추가 | `{테이블}_add_index_{컬럼}` | `messages_add_index_sender` |
| ENUM 값 추가 | `add_enum_{타입}_{값}` | `add_enum_channel_type_stage` |

---

## out-of-order 설정

소수점 서브버전을 사용하면 이미 높은 버전이 적용된 DB에 낮은 서브버전이 추가될 수 있다.
예를 들어 V3~V8이 이미 적용된 상태에서 V2.1을 추가하는 경우다.
이를 허용하기 위해 `out-of-order: true`를 전역 설정한다.

```yaml
# application.yaml
spring:
  flyway:
    out-of-order: true
```

---

## SQL 작성 규칙

### 기본 원칙

- **한 번 적용된 파일은 수정 금지**: 체크섬 불일치로 Flyway가 실행을 거부한다. 변경이 필요하면 새 서브버전 파일을 추가한다.
- **멱등성 없음**: Flyway는 각 파일을 한 번만 실행한다. `IF NOT EXISTS`, `IF EXISTS` 같은 방어 코드는 쓰지 않는다.
- **PostgreSQL 전용 문법 허용**: 운영과 테스트는 모두 PostgreSQL을 사용한다. H2 호환성을 위해 표준 SQL로 제한하지 않는다.

### 타입

| 용도 | 타입 |
|------|------|
| PK | `BIGSERIAL` |
| 일시 | `TIMESTAMPTZ` (시간대 포함) |
| 긴 문자열 | `TEXT` |
| 유한 문자열 | `VARCHAR(n)` |
| 열거형 | PostgreSQL `ENUM` — `V1__create_enum_types.sql`에 `CREATE TYPE`으로 선언 |
| 권한 비트필드 | `BIGINT` |

```sql
// ✅ 올바른 예
id         BIGSERIAL   PRIMARY KEY,
created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
status     user_status NOT NULL DEFAULT 'OFFLINE'

// ❌ 잘못된 예
id         INT         PRIMARY KEY,
created_at TIMESTAMP   NOT NULL,
status     VARCHAR(20) NOT NULL
```

### ENUM 타입 관리

모든 `CREATE TYPE ... AS ENUM`은 `V1__create_enum_types.sql`에 집중한다.
새 ENUM 값을 추가할 때는 `V1`의 서브버전 파일에서 `ALTER TYPE`을 사용한다.

```sql
// ✅ 새 ENUM 값 추가
-- V1.1__add_enum_channel_type_stage.sql
ALTER TYPE channel_type ADD VALUE 'STAGE';

// ❌ 잘못된 예 — V1 파일을 직접 수정
```

### NOT NULL 기본값

`NOT NULL` 컬럼에는 항상 `DEFAULT`를 명시한다.

```sql
// ✅ 올바른 예
is_public  BOOLEAN     NOT NULL DEFAULT FALSE,
uses       INT         NOT NULL DEFAULT 0,
created_at TIMESTAMPTZ NOT NULL DEFAULT now()
```

### ON DELETE 전략

| 관계 | 전략 | 이유 |
|------|------|------|
| 서버 삭제 → 하위 채널·멤버·역할 | `ON DELETE CASCADE` | 서버가 없으면 하위 데이터도 의미 없음 |
| 메시지 삭제 → 첨부·반응 | `ON DELETE CASCADE` | 메시지와 생명주기 동일 |
| 채널 카테고리 삭제 → 채널 | `ON DELETE SET NULL` | 카테고리 없이도 채널 존재 가능 |
| 사용자 삭제 → 보낸 메시지 | 전략 없음 (`RESTRICT` 기본) | 메시지 발신자 기록 보존 필요 |

### 인덱스

FK 컬럼에 인덱스를 추가한다. 조회 패턴이 명확한 경우 복합 인덱스를 추가한다.

```sql
// ✅ 복합 인덱스 예
CREATE INDEX idx_messages_channel_created ON messages (channel_id, created_at DESC);
CREATE INDEX idx_notifications_user_read  ON notifications (user_id, is_read, created_at DESC);
```

### 제약 네이밍

| 종류 | 형식 | 예시 |
|------|------|------|
| UNIQUE | `uk_{테이블명}` 또는 `uk_{테이블명}_{컬럼}` | `uk_server_members`, `uk_users_external_id` |
| CHECK | `chk_{테이블명}_{내용}` | `chk_user_blocks_not_self` |
| FK | 명시 생략 (PostgreSQL 자동 생성) | — |

---

## 프로파일별 Flyway 동작

| 프로파일 | DB | Flyway | DDL Auto |
|---------|-----|--------|----------|
| local | H2 | **비활성** | `create-drop` |
| test (Testcontainers) | PostgreSQL | **활성** | `none` |
| prod | PostgreSQL | **활성** | `none` |

로컬에서 Flyway를 비활성화하는 이유: 마이그레이션 파일은 PostgreSQL 전용 문법(`CREATE TYPE ... AS ENUM` 등)을 사용하므로 H2에서 실행하면 오류가 발생한다.

```yaml
# application-local.yaml
spring:
  flyway:
    enabled: false
  jpa:
    hibernate:
      ddl-auto: create-drop
```

---

## 금지 사항

```sql
// ❌ 적용된 마이그레이션 파일 수정 — 체크섬 불일치
// V2__create_user_tables.sql 내용을 직접 편집

// ❌ 도메인 버전 불일치 — users 변경인데 서버 버전 사용
// V3.1__users_add_bio.sql

// ❌ 서브버전 건너뜀
// V2.1이 이미 있는데 V2.3 추가 (V2.2 없이)

// ❌ DDL과 DML 혼재
// 테이블 생성과 초기 데이터 삽입을 같은 파일에 작성하지 않는다
// 초기 데이터는 별도 파일(V{n}.{m}__insert_initial_{테이블}.sql)로 분리한다
```
