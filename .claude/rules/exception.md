# 예외 처리 규칙

## ErrorCode 도메인 접두사

| 접두사 | 도메인 |
|--------|--------|
| `C` | Common (공통) |
| `R` | Room (채팅방) |
| `M` | Message (메시지) |

새 도메인 추가 시 이 표에 먼저 등록한 뒤 코드를 추가한다.
접두사 내 번호는 001부터 순차 증가하며, 삭제된 번호는 재사용하지 않는다.

```java
// ✅
ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "채팅방을 찾을 수 없습니다."),

// ❌ 접두사 규칙 미준수
ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "room_not_found", "..."),
```

## AppException 사용

모든 비즈니스 예외는 `AppException(ErrorCode)`로 던진다.

```java
// ✅
throw new AppException(ErrorCode.ROOM_NOT_FOUND);

// ❌
throw new RuntimeException("채팅방을 찾을 수 없습니다.");
```

## GlobalExceptionHandler 우선순위

| 순서 | 대상 | 로그 레벨 |
|------|------|-----------|
| 1 | `AppException` | `WARN` |
| 2 | `MethodArgumentNotValidException` | `WARN` |
| 3 | `DataIntegrityViolationException` | `WARN`(UNIQUE 위반) / `ERROR`(그 외) |
| 4 | `ConstraintViolationException` | `WARN` |
| 5 | `NoResourceFoundException` | `DEBUG` |
| 6 | `Exception` (fallback) | `ERROR` |

새 예외 타입 추가 시 이 순서표를 함께 갱신한다.
`Exception` fallback보다 구체적인 타입을 항상 위에 배치한다.

## GlobalExceptionHandler 위치

`com.chat.common.exception` 패키지에 위치한다. (`api` 패키지가 아님)
