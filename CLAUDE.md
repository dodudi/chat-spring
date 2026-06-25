# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Environment

- **JAVA_HOME**: `C:\Users\rudy\.jdks\temurin-21.0.11`

## Build & Test Commands

```bash
# Build
./gradlew build

# Run application
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "kr.it.rudy.chat.some.package.SomeTest"

# Run a single test method
./gradlew test --tests "kr.it.rudy.chat.some.package.SomeTest.methodName"
```

Tests require Docker (for Testcontainers — PostgreSQL, Kafka, Redis are spun up automatically via `TestcontainersConfiguration`).

## Tech Stack

- **Spring Boot 4.1.0**, Java 21, Gradle
- **Security**: OAuth2 Resource Server (JWT) — this app is a *resource server*, not an authorization server
- **Persistence**: JPA + Flyway + PostgreSQL (prod), H2 (local/test override)
- **Messaging**: Kafka, WebSocket
- **Cache**: Redis
- **Testing**: Testcontainers (PostgreSQL, Kafka, Redis), `@WebMvcTest` uses `spring-boot-starter-webmvc-test`

## Package Structure

Root package: `kr.it.rudy.chat`

Packages are organized by **domain**, not by technical layer:

```
kr.it.rudy.chat/
├── common/           shared infrastructure (exception, filter, response, util, config)
└── {domain}/
    ├── domain/       Entity, Repository interface, Enum
    ├── domain/support/  Repository custom impl (Spring Data JPA naming: {Repo}Impl)
    ├── application/  Service interface + impl
    ├── api/          @RestController (JSON responses)
    ├── web/          @Controller (Thymeleaf view names)
    ├── dto/          Request/Response records
    ├── config/       domain-scoped @Configuration
    └── property/     @ConfigurationProperties
```

Never put `@Controller` in `api/` or `@RestController` in `web/`. No central `config/` package — domain configs live inside the domain. `common/config/` is for infrastructure-only config (JPA Auditing, etc.).

## Key Conventions

Convention rules are in `.claude/rules/`. The critical ones:

**DI**: `@RequiredArgsConstructor` + `private final` fields only. No `@Autowired`, no field injection.

**Lombok**: `@RequiredArgsConstructor`, `@Getter`, `@Slf4j` allowed. `@Data` and `@Setter` are **banned**. `@Builder` only on `private` constructors, never exposed on Entity.

**Exceptions**: All business exceptions use `throw new AuthException(ErrorCode.SOME_CODE)`. Never throw `RuntimeException` with a string message. `ErrorCode` enum owns all messages.

**ErrorCode prefixes**: `C`=Common, `U`=User, `T`=Token. New domains get a ≤2-char prefix declared in `exception-conventions.md` before adding codes.

**Exception handlers**: Two separate classes in `common/exception/` — `ApiExceptionHandler` (`@RestControllerAdvice`) for REST, `ViewExceptionHandler` (`@ControllerAdvice`) for Thymeleaf.

**Service naming**: Meaningful names over `Impl` suffix (`SimpleUserService`, `CachedUserService`, `JwtTokenProvider`). Exception: `{Repository}Impl` is required for Spring Data JPA custom impl auto-wiring.

**Responses**:
- Success: `ResponseEntity.ok(ApiResponse.ok(data))`
- Created: `ResponseEntity.created(location).body(ApiResponse.ok(response))`
- Delete: `ResponseEntity.noContent().build()`
- Failure: `ApiResponse.fail(errorCode)` — no separate `ErrorResponse` class

**Logging**: Structured tags — `log.info("[USER_SIGNUP] email={}", email)`. `MDC.put("traceId", ...)` is managed by `RequestLoggingFilter` — do not call it manually.

## Test Conventions

| Test type | Annotation |
|-----------|-----------|
| Service unit | `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks` |
| Controller unit | `@WebMvcTest` + `@MockitoBean` + `@WithMockUser` |
| Repository unit | `@DataJpaTest` |
| Integration | `@SpringBootTest` + Testcontainers |

Test method naming: `메서드명_상황_기대결과` (e.g., `findById_존재하지_않는_id_조회시_AuthException_발생`).

Structure: always use `// given / when / then` comments. Use AssertJ `assertThat` — never JUnit `assertEquals`.

Test classes live in the same package as the class under test.

## History & Troubleshooting Log

`.claude/history.md` records past bugs and non-obvious decisions in reverse-chronological order. Notable entries:
- Thymeleaf `th:each` iteration variable names must not be reserved tokens (`gt`, `lt`, `eq`, `ne`, `and`, `or`, `not`, `div`, `mod`, etc.)
- Thymeleaf 3.1+: `th:onclick` only allows numeric/boolean expressions — use `data-*` attributes + plain `onclick` for String values
