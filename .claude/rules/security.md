# 보안 / 인증 규칙

## 인증 구조

이 서비스는 **OAuth2 Resource Server**로 동작한다. 직접 로그인·회원가입을 처리하지 않는다.

- 클라이언트가 외부 인증 서버에서 발급받은 JWT를 `Authorization: Bearer {token}` 헤더로 전달한다.
- 이 서비스는 토큰을 검증하고 클레임에서 사용자 정보를 추출한다.
- `users` 테이블을 직접 관리하지 않는다.

## 프로파일별 JWT 검증 방식

| 프로파일 | 방식 |
|---------|------|
| `local` | `LocalJwtConfig` — HMAC HS256 (`app.jwt.local-secret`) |
| `prod` | `spring.security.oauth2.resourceserver.jwt.issuer-uri` — 외부 인증 서버 |

## 사용자 정보 접근

```java
@GetMapping("/me")
public ResponseEntity<ApiResponse<MyResponse>> getMe(
        @AuthenticationPrincipal Jwt jwt) {
    String userId = jwt.getSubject();           // 사용자 식별자
    String email  = jwt.getClaimAsString("email");
}
```

## 공개 경로

```java
private static final String[] PUBLIC_PATHS = {
    "/actuator/health",
    "/actuator/info",
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/ws/chat",        // WebSocket 연결 — STOMP CONNECT에서 JWT 검증
    "/h2-console/**"   // 로컬 전용
};
```

새 공개 경로를 추가할 때 `SecurityConfig.PUBLIC_PATHS`에 등록한다.

## 기본 설정

- CSRF 비활성화 (`SessionCreationPolicy.STATELESS`)
- 401 → `ApiResponse` JSON 형식 응답 (`AuthenticationEntryPoint`)
- 403 → `ApiResponse` JSON 형식 응답 (`AccessDeniedHandler`)
