# AOP 규칙

이 파일은 Aspect 작성 시 항상 따라야 할 규칙을 정의한다.

---

## 패키지 위치

Aspect 클래스는 `common/aop/` 패키지에 위치한다.
도메인에 종속된 Aspect는 해당 도메인의 `aop/` 서브패키지에 위치한다.

```
kr.it.rudy.chat/
├── common/
│   └── aop/
│       └── ControllerLoggingAspect.java   ← 공통 인프라 Aspect
└── {domain}/
    └── aop/
        └── SomeDomainAspect.java          ← 도메인 전용 Aspect (필요 시)
```

---

## 클래스 선언

Aspect 클래스는 `@Aspect`, `@Component`, `@Slf4j` 세 어노테이션을 함께 선언한다.

```java
// ✅ 올바른 예
@Aspect
@Component
@Slf4j
public class ControllerLoggingAspect {
    ...
}

// ❌ 잘못된 예
@Aspect
public class ControllerLoggingAspect {   // @Component 없으면 빈으로 등록되지 않음
    ...
}
```

---

## Advice 선택 기준

| Advice | 사용 시점 |
|--------|----------|
| `@Around` | 실행 전·후 모두 제어가 필요할 때 (타이밍, 트랜잭션 등) |
| `@Before` | 실행 전 처리만 필요할 때 (인가 검사 등) |
| `@AfterThrowing` | 예외 발생 시 처리만 필요할 때 (예외 로깅 등) |

`@Around`가 가장 범용적이지만, 용도가 명확하면 더 좁은 Advice를 사용한다.

---

## Pointcut 표현식

| 표현식 | 적용 대상 |
|--------|----------|
| `@within(RestController)` | `@RestController`가 붙은 클래스의 모든 메서드 |
| `@within(Controller)` | `@Controller`가 붙은 클래스의 모든 메서드 |
| `execution(* kr.it.rudy.chat..application.*.*(..))` | application 패키지의 모든 Service 메서드 |
| `@annotation(Transactional)` | 특정 어노테이션이 붙은 메서드 |

Pointcut 표현식이 복잡해지면 `@Pointcut`으로 분리해 이름을 부여한다.

```java
// ✅ Pointcut 분리 — 재사용 가능
@Pointcut("@within(org.springframework.web.bind.annotation.RestController) || @within(org.springframework.stereotype.Controller)")
private void controllers() {}

@Around("controllers()")
public Object logController(ProceedingJoinPoint joinPoint) throws Throwable { ... }

// ❌ 인라인 — 표현식이 길어지면 가독성 저하
@Around("@within(org.springframework.web.bind.annotation.RestController) || @within(org.springframework.stereotype.Controller)")
public Object logController(ProceedingJoinPoint joinPoint) throws Throwable { ... }
```

---

## @Around 구현 패턴

`@Around`는 반드시 `joinPoint.proceed()`의 반환값을 그대로 반환해야 한다.
예외는 반드시 다시 `throw`해야 한다 — 삼켜버리면 예외 전파가 끊긴다.

```java
// ✅ 올바른 예
@Around("controllers()")
public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
    long start = System.currentTimeMillis();
    try {
        Object result = joinPoint.proceed();
        log.info("[CONTROLLER] class={} method={} elapsed={}ms",
                joinPoint.getTarget().getClass().getSimpleName(),
                ((MethodSignature) joinPoint.getSignature()).getMethod().getName(),
                System.currentTimeMillis() - start);
        return result;
    } catch (Exception e) {
        log.warn("[CONTROLLER_ERROR] class={} method={} elapsed={}ms error={}",
                joinPoint.getTarget().getClass().getSimpleName(),
                ((MethodSignature) joinPoint.getSignature()).getMethod().getName(),
                System.currentTimeMillis() - start, e.getMessage());
        throw e;   // 반드시 다시 던진다
    }
}

// ❌ 잘못된 예
@Around("controllers()")
public void logController(ProceedingJoinPoint joinPoint) {   // 반환값 누락
    joinPoint.proceed();
    // 예외를 삼킴 — 호출자에게 전달되지 않음
}
```

---

## 인자(args) 로깅 주의사항

`joinPoint.getArgs()`로 메서드 인자를 로그에 남길 때 아래 사항을 반드시 확인한다.

**민감 정보 마스킹**: 비밀번호·토큰·개인정보가 담긴 파라미터에는 `@Masked`를 붙인다. 해당 인자는 `****`로 대체되어 로그에 찍힌다.

**프레임워크 객체 제외**: `Model`, `HttpServletRequest`, `HttpServletResponse`, `BindingResult`는 로그 출력에서 자동으로 제외된다.

```java
// ✅ @Masked 사용 예 — 컨트롤러 파라미터에 선언
@PostMapping("/login")
public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody @Masked LoginRequest request) {
    // request 전체가 **** 로 출력됨
}

@PostMapping("/users")
public ResponseEntity<ApiResponse<UserResponse>> create(
        @RequestBody CreateUserRequest request,    // 일반 출력
        @Masked @RequestHeader("Authorization") String token  // **** 출력
) { ... }
```

```java
// ✅ formatArgs 구현 패턴 — 프레임워크 객체 제외 + @Masked 마스킹
private String formatArgs(Method method, Object[] args) {
    Annotation[][] paramAnnotations = method.getParameterAnnotations();
    List<String> result = new ArrayList<>();

    for (int i = 0; i < args.length; i++) {
        if (isFrameworkObject(args[i])) {
            continue;
        }
        result.add(isMasked(paramAnnotations[i]) ? "****" : String.valueOf(args[i]));
    }

    return result.toString();
}

private boolean isFrameworkObject(Object arg) {
    return arg instanceof HttpServletRequest
            || arg instanceof HttpServletResponse
            || arg instanceof Model
            || arg instanceof BindingResult;
}

private boolean isMasked(Annotation[] annotations) {
    for (Annotation annotation : annotations) {
        if (annotation instanceof Masked) {
            return true;
        }
    }
    return false;
}
```

---

## 로그 태그 규칙

Aspect 로그도 `[TAG]` 형식을 따른다. 성공·실패를 태그로 구분한다.

| 상황 | 태그 | 레벨 |
|------|------|------|
| 컨트롤러 정상 실행 | `[CONTROLLER]` | `INFO` |
| 컨트롤러 예외 발생 | `[CONTROLLER_ERROR]` | `WARN` |
| 서비스 정상 실행 | `[SERVICE]` | `INFO` |
| 서비스 예외 발생 | `[SERVICE_ERROR]` | `WARN` |

---

## 금지 사항

```java
// ❌ Aspect 내에서 비즈니스 로직 수행
@Around("controllers()")
public Object doSomething(ProceedingJoinPoint joinPoint) throws Throwable {
    userService.incrementCallCount();   // Aspect는 횡단 관심사만 처리
    return joinPoint.proceed();
}

// ❌ Spring 빈 의존성을 @Autowired 필드 주입으로 선언
@Aspect
@Component
public class SomeAspect {
    @Autowired
    private UserService userService;    // DI 규칙 위반 — @RequiredArgsConstructor + final 사용
}

// ❌ @Pointcut 없이 복잡한 표현식을 여러 Advice에 중복 작성
@Before("execution(* kr.it.rudy.chat..api..*(..)) || execution(* kr.it.rudy.chat..web..*(..))")
// @Around("execution(* kr.it.rudy.chat..api..*(..)) || execution(* kr.it.rudy.chat..web..*(..))")
// → 동일 표현식을 @Pointcut으로 분리해 재사용한다
```
