# API Design

## URL 네이밍 규칙

- 소문자 + 하이픈(`-`) 사용, 언더스코어 금지
- 명사 복수형 사용, 동사 금지
- 도메인 리소스 중심으로 구성

```
# Good
GET    /api/v1/users
GET    /api/v1/users/{id}
POST   /api/v1/users
PUT    /api/v1/users/{id}
DELETE /api/v1/users/{id}

GET    /api/v1/users/{id}/orders    # 중첩 리소스 (최대 2단계)

# Bad
GET    /api/v1/getUser
POST   /api/v1/createUser
GET    /api/v1/user_list
```

## HTTP 메서드 사용 기준

| 메서드 | 용도 | 멱등성 |
|--------|------|--------|
| GET | 조회 | O |
| POST | 생성 | X |
| PUT | 전체 수정 | O |
| PATCH | 부분 수정 | O |
| DELETE | 삭제 | O |

- 상태 변경(승인, 취소 등)은 PATCH 사용
- 검색 조건이 복잡한 경우 POST + `/search` 허용

## 버저닝

- URL Path 방식 사용: `/api/v1/...`
- 하위 호환성이 깨지는 변경 시에만 버전 올림

## 요청/응답 포맷

### 목록 조회 (페이지네이션)

```json
// 요청
GET /api/v1/users?page=0&size=20&sort=createdAt,desc

// 응답
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "last": false
  }
}
```

### 단건 조회

```json
GET /api/v1/users/1

{
  "success": true,
  "data": {
    "id": 1,
    "name": "홍길동",
    "email": "hong@example.com"
  }
}
```

### 생성

```json
POST /api/v1/users
// 201 Created 반환
// Location 헤더에 생성된 리소스 URI 포함
Location: /api/v1/users/1

{
  "success": true,
  "data": {
    "id": 1
  }
}
```

### 수정/삭제

```
// 204 No Content 반환 (응답 바디 없음)
```

## Controller 작성 규칙

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUser(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateUserResponse>> createUser(
            @RequestBody @Valid CreateUserRequest request) {
        CreateUserResponse response = userService.createUser(request);
        URI location = URI.create("/api/v1/users/" + response.getId());
        return ResponseEntity.created(location).body(ApiResponse.ok(response));
    }
}
```

## 쿼리 파라미터 네이밍

- camelCase 사용 금지, snake_case 또는 camelCase 중 하나로 통일 (프로젝트 시작 시 결정)
- 정렬: `sort=fieldName,asc|desc`
- 필터: 필드명 그대로 (`status=ACTIVE`, `createdAfter=2024-01-01`)

## 날짜/시간 포맷

- ISO 8601 형식 사용: `2024-01-15T09:30:00Z`
- 타임존은 UTC 기준으로 주고받음
- 모든 날짜/시간 타입은 `OffsetDateTime` 사용

```java
// ✅ Entity 필드
private OffsetDateTime createdAt;

// ✅ DTO 응답 — UTC offset 명시
public record MessageResponse(Long id, String content, OffsetDateTime sentAt) { }

// ✅ 현재 시각 생성
OffsetDateTime.now(ZoneOffset.UTC)

// ❌ 사용 금지
LocalDateTime  // offset 없음 — DB 저장 시 타임존 소실
ZonedDateTime  // 타임존 규칙(DST 등) 불필요 — API 통신은 항상 UTC
Instant        // Hibernate 6 + PostgreSQL TIMESTAMPTZ 매핑 시 드라이버 설정 의존성 발생
```

**Jackson 직렬화 결과**

```json
"sentAt": "2024-01-15T09:30:00Z"
```