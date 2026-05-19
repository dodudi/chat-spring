# API 규칙

## Controller 기본 구조

```java
@RestController
@RequestMapping("/api/v1/{resource}")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomSummaryResponse>> getRoom(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(roomService.findById(id)));
    }
}
```

- `@RestController` + `@RequestMapping` + `@RequiredArgsConstructor` 세 어노테이션을 함께 사용한다.
- 반환 타입은 항상 `ResponseEntity<ApiResponse<T>>`다.
- Entity를 직접 반환하지 않는다.

## 응답 형식

### 성공

```java
// 조회
return ResponseEntity.ok(ApiResponse.ok(data));

// 생성
URI location = URI.create("/api/v1/rooms/" + response.id());
return ResponseEntity.created(location).body(ApiResponse.ok(response));

// 삭제
return ResponseEntity.noContent().build();
```

### 실패

```json
{
  "code": "R001",y
  "message": "채팅방을 찾을 수 없습니다.",
  "data": null
}
```

## HTTP 상태코드 기준

| 상황 | 상태코드 |
|------|---------|
| 조회 성공 | `200 OK` |
| 생성 성공 | `201 Created` + `Location` 헤더 |
| 삭제 성공 | `204 No Content` |
| 유효성 오류 | `400 Bad Request` |
| 인증 없음 | `401 Unauthorized` |
| 권한 없음 | `403 Forbidden` |
| 리소스 없음 | `404 Not Found` |

## Request DTO

- `record`를 우선 사용한다.
- Validation 어노테이션을 DTO에 직접 선언한다.
- Controller 파라미터에 `@Valid`를 붙인다.

```java
public record CreateGroupRoomRequest(
        @NotBlank String name,
        @Size(min = 2) List<String> memberIds
) {}
```
