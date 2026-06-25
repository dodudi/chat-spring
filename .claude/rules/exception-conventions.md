# 예외 · ErrorCode 규칙

이 파일은 예외 처리와 에러 코드 작성 시 항상 따라야 할 규칙을 정의한다.

---

## ErrorCode 도메인 접두사

| 접두사 | 도메인 | 예시 |
|--------|--------|------|
| `C` | Common (공통) | `C001`, `C002` |
| `U` | User (사용자) | `U001`, `U002` |
| `CL` | Client (OAuth2 클라이언트) | `CL001`, `CL002` |
| `T` | Token (토큰) | `T001`, `T002` |

새 도메인을 추가할 때는 2자리 이내 영문 접두사를 먼저 이 표에 정의한 뒤 코드를 추가한다.
접두사 내 번호는 001부터 순차 증가한다. 삭제된 번호는 재사용하지 않는다.

```java
// ✅ 올바른 예
PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "결제 내역을 찾을 수 없습니다."),

// ❌ 잘못된 예
PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "payment_not_found", "결제 내역을 찾을 수 없습니다."),  // 접두사 규칙 미준수
PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "U010", "결제 내역을 찾을 수 없습니다."),              // 도메인 불일치
```

---

## AuthException 사용 규칙

모든 비즈니스 예외는 `AuthException(ErrorCode)`로 던진다.
`RuntimeException`을 직접 throw하거나 문자열 메시지로 예외를 생성하지 않는다.

```java
// ✅ 올바른 예
throw new AuthException(ErrorCode.USER_NOT_FOUND);

// ❌ 잘못된 예
throw new RuntimeException("사용자를 찾을 수 없습니다.");   // RuntimeException 직접 사용
throw new AuthException("사용자를 찾을 수 없습니다.");      // 문자열 생성자 없음 — 컴파일 오류
```

`AuthException`에 생성자를 추가하지 않는다. `ErrorCode`가 메시지를 관리한다.

---

## 응답 형식

### 성공 응답

```java
// 조회
return ResponseEntity.ok(ApiResponse.ok(data));

// 생성
URI location = URI.create("/api/v1/users/" + response.id());
return ResponseEntity.created(location).body(ApiResponse.ok(response));

// 삭제
return ResponseEntity.noContent().build();
```

### 실패 응답

`ApiResponse<Void>`를 사용한다. `ErrorResponse` 클래스는 존재하지 않는다.

```java
// GlobalExceptionHandler 내부
return ResponseEntity
        .status(e.getErrorCode().getHttpStatus())
        .body(ApiResponse.fail(e.getErrorCode()));

// 메시지를 덮어써야 할 때 (Validation 등)
return ResponseEntity
        .status(ErrorCode.INVALID_INPUT.getHttpStatus())
        .body(ApiResponse.fail(ErrorCode.INVALID_INPUT, "필드명: 상세 메시지"));
```

실패 응답 JSON 형식:
```json
{
  "code": "U001",
  "message": "사용자를 찾을 수 없습니다.",
  "data": null
}
```

---

## ExceptionHandler 위치 및 구조

핸들러는 역할에 따라 두 클래스로 분리한다. 모두 `common/exception` 패키지에 위치한다.

```
kr.it.rudy.admin.common.exception/
├── AuthException.java
├── ErrorCode.java
├── ApiExceptionHandler.java    ← @RestController 전용 (JSON 응답)
└── ViewExceptionHandler.java   ← @Controller 전용 (뷰 이름 반환)
```

---

## ApiExceptionHandler — REST API 예외 처리

`@RestControllerAdvice(annotations = RestController.class)`로 `@RestController`에서 발생한 예외만 처리한다.

처리 핸들러 우선순위:

| 순서 | 대상 | 로그 레벨 |
|------|------|-----------|
| 1 | `AuthException` | `WARN` |
| 2 | `MethodArgumentNotValidException` | `WARN` |
| 3 | `NoResourceFoundException` | `DEBUG` |
| 4 | `Exception` (fallback) | `ERROR` |

```java
// ✅ 올바른 예
@RestControllerAdvice(annotations = RestController.class)
@RequiredArgsConstructor
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException e) {
        log.warn("[AUTH_EXCEPTION] {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.fail(e.getErrorCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[UNHANDLED_EXCEPTION]", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
```

새 예외 타입을 추가할 때는 위 순서표를 함께 갱신한다.
`Exception` fallback보다 구체적인 타입을 항상 위에 배치한다.

---

## ViewExceptionHandler — Thymeleaf 뷰 예외 처리

`@ControllerAdvice(annotations = Controller.class)`로 `@Controller`에서 발생한 예외만 처리한다.
핸들러 메서드는 뷰 이름(String)을 반환하고 에러 정보를 `Model`에 담는다.

```java
// ✅ 올바른 예
@ControllerAdvice(annotations = Controller.class)
@Slf4j
public class ViewExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public String handleAuthException(AuthException e, Model model) {
        log.warn("[VIEW_AUTH_EXCEPTION] {}", e.getMessage());
        model.addAttribute("errorMessage", e.getErrorCode().getMessage());
        return "error/common";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        log.error("[VIEW_UNHANDLED_EXCEPTION]", e);
        model.addAttribute("errorMessage", "서버 오류가 발생했습니다.");
        return "error/500";
    }
}
```

에러 뷰 템플릿은 `templates/error/` 하위에 위치한다.

```
src/main/resources/templates/error/
├── common.html   ← 비즈니스 예외 공통
└── 500.html      ← 서버 오류
```

```java
// ❌ 잘못된 예 — @RestControllerAdvice 하나로 모두 처리
@RestControllerAdvice                          // @Controller 예외도 JSON으로 응답됨
public class GlobalExceptionHandler { ... }
```
