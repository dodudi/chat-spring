# 코딩 스타일 규칙

## Lombok

| 어노테이션 | 사용 |
|-----------|------|
| `@RequiredArgsConstructor` | ✅ 항상 사용. DI는 `final` 필드 + 이 어노테이션으로 처리 |
| `@Getter` | ✅ 허용 |
| `@Slf4j` | ✅ 로그가 필요한 클래스에 선언 |
| `@Data` | ❌ 금지. `@Setter` 포함으로 Entity 불변성 파괴 |
| `@Setter` | ❌ 금지. 상태 변경은 의미 있는 메서드로 표현 |
| `@Builder` | ⚠️ `private` 생성자에만 적용. Entity 외부 빌더 노출 금지 |
| `@Autowired` | ❌ 필드 주입 금지 |

## 네이밍

- 클래스: `PascalCase`
- 메서드·변수: `camelCase`
- 상수: `UPPER_SNAKE_CASE`
- 패키지: 소문자 단어

## 상수

매직 넘버·문자열은 상수로 선언한다. 클래스 내부에서만 쓰이면 `private static final`, 공유가 필요하면 별도 상수 클래스로 분리한다.

```java
// ✅
private static final String KEY_PREFIX = "user:online:";
private static final Duration ONLINE_TTL = Duration.ofSeconds(60);

// ❌
redisTemplate.opsForValue().set("user:online:" + userId, "1");
```

## 유틸리티 클래스

인스턴스화가 불필요한 유틸리티 클래스는 `final` 선언 + `private` 생성자를 추가한다.

```java
// ✅
public final class HttpUtils {
    private HttpUtils() {}
    public static String getClientIp(HttpServletRequest request) { ... }
}
```

## 불변성

- Entity에 `@Setter` / `@Data` 사용 금지
- 상태 변경은 의미 있는 `public` 메서드로만 표현
- 생성은 정적 팩토리 메서드(`create`, `of`)로만 허용
