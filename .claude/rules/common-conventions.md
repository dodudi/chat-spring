# 공통 컨벤션 규칙

모든 레이어에서 공통으로 적용되는 규칙을 정의한다.

---

## Lombok

| 어노테이션 | 사용 | 비고 |
|-----------|------|------|
| `@RequiredArgsConstructor` | ✅ 항상 사용 | DI는 `final` 필드 + 이 어노테이션으로 처리 |
| `@Getter` | ✅ 허용 | Entity, DTO 외 클래스에 사용 |
| `@Slf4j` | ✅ 허용 | 로그가 필요한 클래스에 선언 |
| `@Data` | ❌ 금지 | `@Setter` 포함으로 Entity 불변성 파괴 |
| `@Setter` | ❌ 금지 | 상태 변경은 의미 있는 메서드로 표현 |
| `@Builder` | ⚠️ 제한 | `private` 생성자에만 적용, Entity 외부 빌더 노출 금지 |

```java
// ✅ 올바른 예
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
}

// ❌ 잘못된 예
@Data                        // @Setter 포함 — Entity에 사용 금지
public class User { ... }

@Autowired                   // 필드 주입 금지
private UserService userService;
```

---

## 유틸리티 클래스

인스턴스화가 불필요한 유틸리티 클래스는 `private` 생성자를 선언한다.

```java
// ✅ 올바른 예
public final class HttpUtils {
    private HttpUtils() {}

    public static String getClientIp(HttpServletRequest request) { ... }
}

// ❌ 잘못된 예
public class HttpUtils {                   // final 없음
    public static String getClientIp() {} // 생성자 미차단
}
```

---

## 상수

매직 넘버·문자열은 상수로 선언한다.
상수는 해당 클래스 내부에 `private static final`로 선언하거나, 여러 클래스에서 공유할 경우 별도 상수 클래스를 만든다.

```java
// ✅ 올바른 예
private static final String EMAIL_VERIFY_KEY_PREFIX = "email:verify:";
private static final int MAX_RETRY_COUNT = 5;

// ❌ 잘못된 예
redisTemplate.opsForValue().set("email:verify:" + token, email);  // 매직 문자열
