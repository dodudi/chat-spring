# Troubleshooting

이 프로젝트에서 실제로 발생한 문제와 해결 방법을 기록한다.
같은 문제를 반복하지 않기 위한 참고 문서.

---

## #001 Spring Boot 4.x — `@WebMvcTest` import 경로 변경

**증상**
`@WebMvcTest` 사용 시 `ClassNotFoundException` 또는 IDE에서 import를 찾지 못함.

**원인**
Spring Boot 4.0에서 테스트 스타터가 모듈화되면서 WebMVC 슬라이스 관련 클래스가 분리됨.

**해결**
```java
// ❌ Spring Boot 3.x
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

// ✅ Spring Boot 4.x
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
```

---

## #002 Spring Boot 4.x — `@WebMvcTest`에서 `ObjectMapper` 주입 불가

**증상**
`@WebMvcTest` 슬라이스 컨텍스트에서 `@Autowired ObjectMapper` 주입 실패.

**원인**
Spring Boot 4.x의 WebMVC 슬라이스에서 `JacksonAutoConfiguration`이 포함되지 않아 `ObjectMapper` 빈이 등록되지 않음.

**해결**
테스트 클래스에서 직접 인스턴스 생성:
```java
private final ObjectMapper objectMapper = new ObjectMapper();
```

---

## #003 Spring Boot 4.x — 테스트 내 `@RestController`가 `@WebMvcTest` 스캔에서 제외

**증상**
테스트 클래스 내부에 정의한 `static @RestController`가 MockMvc에서 404 반환.

**원인**
`@WebMvcTest`는 test 소스의 중첩 클래스를 컴포넌트 스캔하지 않음.

**해결**
`@Import`로 명시적 등록:
```java
@WebMvcTest
@Import({GlobalExceptionHandler.class, MyTest.TestController.class})
class MyTest { ... }
```

---

## #004 Spring Boot 4.x — `@MockBean` 제거, `@MockitoBean` 사용

**증상**
`@MockBean` import 오류 또는 `NoSuchBeanDefinitionException`.

**원인**
Spring Boot 4.0에서 Boot 전용 `@MockBean`이 제거되고 Spring Test 레벨의 `@MockitoBean`으로 대체됨.

**해결**
```java
// ❌ Spring Boot 3.x
import org.springframework.boot.test.mock.mockito.MockBean;
@MockBean UserService userService;

// ✅ Spring Boot 4.x
import org.springframework.test.context.bean.override.mockito.MockitoBean;
@MockitoBean UserService userService;
```

---

## #005 Spring Boot 4.x — `ObjectMapper` 빈 미등록 (`@SpringBootTest` 포함)

**증상**
```
NoSuchBeanDefinitionException: No qualifying bean of type 'ObjectMapper'
```
`ChatMessagePublisher`, `ChatMessageSubscriber` 등 `ObjectMapper`를 생성자 주입받는 빈 생성 실패.

**원인**
Spring Boot 4.x에서 `spring-boot-starter-webmvc` 환경에서 `JacksonAutoConfiguration`이 `ObjectMapper` 빈을 자동 등록하지 않는 케이스 존재. (#002와 다름 — 테스트 슬라이스가 아닌 실 컨텍스트에서도 발생)

**해결**
`JacksonConfig.java`를 명시적으로 작성해 `ObjectMapper` 빈 등록:
```java
@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
```

---

## #006 `@SpringBootTest` — Reactive Redis 자동구성 충돌

**증상**
```
BeanNotOfRequiredTypeException: Bean named 'redisConnectionFactory' is expected to be
of type 'ReactiveRedisConnectionFactory' but was actually of type 'RedisConnectionFactory$MockitoMock'
```

**원인**
Spring Boot 4.x `DataRedisReactiveAutoConfiguration`이 `redisConnectionFactory` 빈을 `ReactiveRedisConnectionFactory`로 사용하려 하지만, `@MockitoBean RedisConnectionFactory`로 생성한 Mockito mock은 `ReactiveRedisConnectionFactory`를 구현하지 않음.

**해결**
`@MockitoBean ReactiveRedisConnectionFactory` 별도 추가 + 헬스체크 비활성화:
```java
@MockitoBean RedisConnectionFactory redisConnectionFactory;
@MockitoBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
```
```java
@SpringBootTest(properties = {
    "management.health.redis.enabled=false"
})
```

---

## #007 `@WebMvcTest` — `StompExceptionAdvice`가 `SimpMessagingTemplate` 요구

**증상**
```
NoSuchBeanDefinitionException: No qualifying bean of type 'SimpMessagingTemplate'
```
`@WebMvcTest` 테스트 컨텍스트 로딩 실패.

**원인**
`StompExceptionAdvice`는 `@ControllerAdvice`이므로 `@WebMvcTest` 슬라이스에서 항상 로딩된다. 이 클래스가 `SimpMessagingTemplate`을 생성자 주입받는데 WebMVC 슬라이스에는 해당 빈이 없음.

**해결**
모든 `@WebMvcTest` 테스트 클래스에 추가:
```java
@MockitoBean SimpMessagingTemplate simpMessagingTemplate;
```
`@WebMvcTest`(컨트롤러 미지정) 형태는 `StompChatHandler(@Controller)`도 로딩하므로 추가로 필요:
```java
@MockitoBean ChatMessagePublisher chatMessagePublisher;
@MockitoBean PresenceService presenceService;
```

---

## #008 `@WebMvcTest` — `@AuthenticationPrincipal Jwt` 사용 시 403

**증상**
`@WithMockUser`를 사용해도 컨트롤러 메서드에서 `Jwt jwt` 파라미터가 null → NPE → 403 반환.

**원인**
`@WithMockUser`는 `UsernamePasswordAuthenticationToken`을 생성하므로 `@AuthenticationPrincipal Jwt`로 주입받으면 null이 된다.

**해결**
`SecurityMockMvcRequestPostProcessors.jwt()`로 교체:
```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

mockMvc.perform(get("/api/v1/rooms")
        .with(jwt().jwt(j -> j.subject("user-a"))))
```
`@MockitoBean JwtDecoder jwtDecoder`도 함께 추가 필요.

---

## #009 `RoomServiceImplTest` — `existing.getId()` NullPointerException

**증상**
```
NullPointerException at RoomServiceImpl.java
filter(p -> existing.getId().equals(p.getId()))
```

**원인**
`ChatRoom.createDm()`으로 생성한 엔티티는 DB를 거치지 않아 `id`가 null. `existing.getId().equals(...)` 호출 시 NPE 발생.

**해결**
테스트 헬퍼에서 reflection으로 id 세팅:
```java
private ChatRoom dmRoom(Long id) {
    ChatRoom room = ChatRoom.createDm("user-a", "user-b");
    var f = ChatRoom.class.getDeclaredField("id");
    f.setAccessible(true);
    f.set(room, id);
    return room;
}
```

---

## #010 `MessageControllerTest` — `@Max(100) int size` 위반 시 500

**증상**
`size=101` 요청 시 400이 아닌 500 반환.

**원인**
`@Validated` 컨트롤러의 `@RequestParam`에서 발생하는 `ConstraintViolationException`이 `GlobalExceptionHandler`에서 처리되지 않아 500으로 떨어짐.

**해결**
`GlobalExceptionHandler`에 핸들러 추가:
```java
@ExceptionHandler(ConstraintViolationException.class)
public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException e) {
    return ResponseEntity.status(INVALID_INPUT.getHttpStatus())
            .body(ApiResponse.fail(INVALID_INPUT));
}
```

**주의**
import는 `jakarta.validation.ConstraintViolationException` — Hibernate의 것과 혼동 주의.

---

## #011 H2 + Flyway — `GENERATED ALWAYS AS IDENTITY` 문법 오류

**증상**
```
SQL State  : 42001
Syntax error in SQL statement "... PRIMARY KEY [*]GENERATED ALWAYS AS IDENTITY ..."
```
로컬에서 `spring.profiles.active=local`로 실행 시 Flyway가 V1 마이그레이션에서 실패.

**원인**
`MODE=PostgreSQL` 옵션을 설정해도 H2 2.4.x는 `PRIMARY KEY GENERATED ALWAYS AS IDENTITY` 인라인 문법을 지원하지 않음. PostgreSQL 10+ 전용 문법.

**해결**
`application-local.yml`에서 Flyway를 비활성화하고 JPA `ddl-auto: create-drop` 사용:
```yaml
spring:
  flyway:
    enabled: false
  jpa:
    hibernate:
      ddl-auto: create-drop
```

**주의**
H2 호환 문법으로 마이그레이션 파일을 수정하면 안 됨 — 운영 PostgreSQL 환경과 불일치 발생.

---

## #012 H2 콘솔 — iframe 렌더링 차단

**증상**
`/h2-console` 접속 시 페이지가 빈 화면이거나 콘솔 UI가 표시되지 않음.

**원인**
H2 콘솔은 iframe을 사용해 UI를 구성한다. Spring Security의 기본 `X-Frame-Options: DENY` 설정이 iframe 로딩을 차단함.
또한 `/h2-console/**`이 `PUBLIC_PATHS`에 없으면 인증 필터에서 401을 반환함.

**해결**
`SecurityConfig.java`에 두 가지 추가:
```java
private static final String[] PUBLIC_PATHS = {
    ...
    "/h2-console/**"  // 추가
};

http
    .headers(headers -> headers
        .frameOptions(frame -> frame.sameOrigin()))  // iframe 허용
```

**주의**
`sameOrigin()`은 같은 출처의 iframe만 허용한다. 운영 환경에서는 `spring.h2.console.enabled=false`로 비활성화해야 함.

---

## #013 `application-local.yml` — `spring.security.oauth2.resourceserver.jwt` 빈 바인딩 오류

**증상**
```
Failed to bind properties under 'spring.security.oauth2.resourceserver.jwt' to
OAuth2ResourceServerProperties$Jwt:
  Reason: ConverterNotFoundException
```
앱 기동 실패.

**원인**
`jwt:` 키만 남기고 하위 프로퍼티를 모두 주석처리하면 Spring Boot가 빈 객체로 바인딩을 시도하다 실패한다.
`LocalJwtConfig`에서 `@Bean JwtDecoder`를 직접 등록하므로 이 블록 자체가 불필요함.

**해결**
`application-local.yml`에서 `spring.security.oauth2.resourceserver.jwt` 블록 전체 제거:
```yaml
# ❌ 잘못된 예 — jwt: 키만 남기고 값이 없는 경우
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # issuer-uri 주석처리

# ✅ 올바른 예 — 블록 자체를 제거
app:
  jwt:
    local-secret: "local-dev-secret-key-must-be-32-chars!!"
```

---

## #014 Postman Pre-request Script — CryptoJS `sigBytes` TypeError

**증상**
```
TypeError: Cannot read properties of undefined (reading 'sigBytes')
```
Postman Console에서 Pre-request Script 실행 시 오류 발생, 토큰 미생성.

**원인**
`pm.environment.get('localJwtSecret')`이 `undefined`를 반환할 때(환경 변수 미설정) CryptoJS.HmacSHA256에 `undefined`가 키로 전달되어 내부 처리 중 크래시 발생.

**해결**
폴백 기본값 추가:
```javascript
// ❌ 잘못된 예
const secret = pm.environment.get('localJwtSecret');

// ✅ 올바른 예
const secret = pm.environment.get('localJwtSecret')
    || 'local-dev-secret-key-must-be-32-chars!!';
```

**주의**
Postman Environment에 `localJwtSecret` 변수를 추가해두면 폴백 없이도 동작한다. 팀원마다 다른 시크릿을 써야 한다면 환경 변수로 관리할 것.

---

## #015 nginx — WebSocket 연결 실패 (101 미반환)

**증상**
브라우저에서 `wss://chat.rudy.it.kr/ws/chat` 연결 시 "WebSocket connection failed" 오류.
curl로 HTTP 요청은 정상이나 WebSocket 업그레이드가 안 됨.

**원인**
nginx 기본 설정은 HTTP/1.0으로 프록시하며 `Upgrade` 헤더를 전달하지 않아 WebSocket 핸드셰이크가 실패함.

**해결**
nginx `location` 블록에 다음 추가:
```nginx
proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
proxy_read_timeout 3600s;
```

**검증**
```bash
curl -k -i -N \
  -H "Connection: Upgrade" -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Version: 13" \
  -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
  https://chat.rudy.it.kr/ws/chat
# → HTTP/1.1 101 Switching Protocols 응답이 와야 정상
```

---

## #016 nginx — `host not found in upstream "chat-spring"`

**증상**
nginx 설정 변경 후 기동 실패:
```
nginx: [emerg] host not found in upstream "chat-spring"
```

**원인**
nginx가 Docker 컨테이너로 실행 중이어서 `chat-spring` 호스트명은 같은 Docker 네트워크(`chat-net`) 안에서만 해석됨. 호스트에서 직접 실행한 nginx라면 해당 이름을 모름.

**해결**
nginx도 `chat-net`에 속한 컨테이너로 실행되어야 한다. `proxy_pass http://chat-spring:8080`은 그대로 유지. `localhost:21002`로 바꾸면 안 됨 — 컨테이너 내부 localhost는 호스트가 아님.

**주의**
nginx reload 시 반드시 컨테이너 내부에서 실행해야 한다:
```bash
docker exec <nginx컨테이너명> nginx -s reload
```
호스트에서 `sudo nginx -s reload`를 실행하면 컨테이너 nginx에 적용되지 않음.

---

## #017 STOMP CONNECT — JWT issuer 불일치로 인증 실패

**증상**
WebSocket 연결(101)은 성공하나 STOMP CONNECT 직후 ERROR 프레임 수신:
```
Failed to send message to ExecutorSubscribableChannel[clientInboundChannel]
```
Spring Boot 로그에 `[WS_AUTH_FAIL]`이 찍히지 않음.

**원인**
`client_credentials`로 발급된 JWT의 `iss` 클레임이 `http://auth.rudy.it.kr` (HTTP)인데,
Spring Boot의 `JWT_ISSUER_URI`는 `https://auth.rudy.it.kr` (HTTPS)로 설정되어 있어 issuer 검증 실패.

토큰 payload 디코딩으로 확인:
```json
{ "iss": "http://auth.rudy.it.kr" }
```

**해결**
Spring Authorization Server의 issuer를 HTTPS로 명시 설정:
```yaml
# auth 서버 application.yml
spring:
  security:
    oauth2:
      authorizationserver:
        issuer: https://auth.rudy.it.kr
```
auth 서버 재시작 후 새로 발급한 토큰의 `iss`가 `https://auth.rudy.it.kr`이면 정상.

**주의**
`JwtChannelInterceptor`가 `JwtException`이 아닌 예외를 받으면 `[WS_AUTH_FAIL]` 로그가 찍히지 않을 수 있음. 로그가 없어도 STOMP ERROR가 오면 JWT 문제를 먼저 의심할 것.

---

## #018 test-client — CORS로 OIDC Discovery 차단

**증상**
IntelliJ 내장 서버(랜덤 포트 예: 63342)에서 HTML을 열면:
```
Access to fetch at 'https://auth.rudy.it.kr/.well-known/openid-configuration'
has been blocked by CORS policy
```

**원인**
브라우저의 `fetch()`는 CORS 정책을 따른다. auth 서버가 `localhost:63342`를 허용하지 않아 차단됨. `localhost:3000`은 허용됨.

**해결**
OIDC auto-discovery `fetch()`를 제거하고 `client_credentials` 방식으로 전환:
- Authorization Endpoint / Token Endpoint 를 직접 입력
- `Authorization: Basic base64(clientId:clientSecret)` 헤더로 토큰 발급
- redirect 없이 POST 한 번으로 토큰 수령

**확인 방법**
```bash
curl -H "Origin: http://localhost:3000" -I https://auth.rudy.it.kr/.well-known/openid-configuration
# Access-Control-Allow-Origin: http://localhost:3000 이면 허용됨
```

---

## #019 RoomSummaryProjection — Instant/OffsetDateTime 타입 불일치

**증상**
`/api/v1/rooms` (내 방 목록 조회) 호출 시 런타임 오류 발생:
```
Cannot project java.time.Instant to java.time.OffsetDateTime;
Target type is not an interface and no matching Converter found
```

**원인**
PostgreSQL `TIMESTAMPTZ` 컬럼을 native query + Spring Data JPA 인터페이스 프로젝션으로 조회하면 Hibernate 7.x가 `Instant`로 매핑한다.
`RoomSummaryProjection`이 `OffsetDateTime`을 반환 타입으로 선언하고 있어 프로젝션 변환 불가.

**해결**
`RoomSummaryProjection`의 반환 타입을 `Instant`로 변경하고, `RoomSummaryResponse.from()`에서 `toOffsetDateTime(Instant)` 헬퍼로 변환:
```java
// RoomSummaryProjection.java
Instant getUpdatedAt();
Instant getLastMessageAt();

// RoomSummaryResponse.java
private static OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
}
```

