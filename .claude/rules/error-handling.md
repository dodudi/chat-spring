# Error Handling

## 공통 응답 포맷

모든 API 응답은 아래 포맷을 따른다.

```json
// 성공
{
  "success": true,
  "data": { ... }
}

// 실패
{
  "success": false,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "사용자를 찾을 수 없습니다."
  }
}
```

## 공통 응답 클래스

```java
// global/response/ApiResponse.java
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorResponse error;

    private ApiResponse(T data) {
        this.success = true;
        this.data = data;
        this.error = null;
    }

    private ApiResponse(ErrorResponse error) {
        this.success = false;
        this.data = null;
        this.error = error;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data);
    }

    public static ApiResponse<Void> fail(ErrorResponse error) {
        return new ApiResponse<>(error);
    }
}
```

## 커스텀 예외 구조

```java
// global/exception/BusinessException.java
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

```java
// global/exception/ErrorCode.java (enum)
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 오류가 발생했습니다."),

    // 사용자
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER_002", "이미 사용 중인 이메일입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

## 전역 예외 핸들러

```java
// global/exception/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 커스텀 비즈니스 예외
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(ErrorResponse.of(errorCode)));
    }

    // Bean Validation 실패 (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("입력값이 올바르지 않습니다.");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(new ErrorResponse("COMMON_001", message)));
    }

    // 그 외 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR)));
    }
}
```

## HTTP 상태코드 사용 기준

| 상황 | 상태코드 |
|------|---------|
| 조회 성공 | 200 OK |
| 생성 성공 | 201 Created |
| 수정/삭제 성공 (응답 없음) | 204 No Content |
| 잘못된 입력값 | 400 Bad Request |
| 인증 필요 | 401 Unauthorized |
| 권한 없음 | 403 Forbidden |
| 리소스 없음 | 404 Not Found |
| 중복/충돌 | 409 Conflict |
| 서버 오류 | 500 Internal Server Error |

## 예외 발생 규칙

- 서비스 레이어에서 `BusinessException`을 throw
- 컨트롤러에서 예외를 직접 catch하지 않음 (GlobalExceptionHandler에 위임)
- `NullPointerException`, `IllegalArgumentException` 등 시스템 예외는 서비스 레이어에서 방어 코드로 처리 후 `BusinessException`으로 감싸서 throw
- 에러 코드는 `ErrorCode` enum에만 정의하고 문자열 하드코딩 금지