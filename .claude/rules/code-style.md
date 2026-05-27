# Code Style Guide — Spring / Java

> 이 문서는 Spring Framework 기반 Java 프로젝트의 코드 스타일 가이드입니다.  
> 팀 전체가 일관된 코드를 작성할 수 있도록 기준을 제시합니다.

---

## 목차

1. [기본 원칙](#1-기본-원칙)
2. [포맷팅](#2-포맷팅)
3. [네이밍 컨벤션](#3-네이밍-컨벤션)
4. [패키지 및 임포트](#4-패키지-및-임포트)
5. [클래스 설계](#5-클래스-설계)
6. [메서드 설계](#6-메서드-설계)
7. [Spring 레이어 규칙](#7-spring-레이어-규칙)
8. [예외 처리](#8-예외-처리)
9. [로깅](#9-로깅)
10. [테스트 코드](#10-테스트-코드)
11. [주석 및 문서화](#11-주석-및-문서화)
12. [금지 사항](#12-금지-사항)

---

## 1. 기본 원칙

- **가독성 우선**: 코드는 작성보다 읽히는 횟수가 훨씬 많다.
- **단일 책임 원칙(SRP)**: 클래스와 메서드는 하나의 역할만 담당한다.
- **일관성**: 개인 취향보다 팀 합의된 규칙을 따른다.
- **명시성**: 암묵적인 동작보다 명확한 코드를 선호한다.

---

## 2. 포맷팅

### 들여쓰기 및 공백

- 들여쓰기: **스페이스 4칸** (탭 사용 금지)
- 줄 길이: 최대 **120자**
- 파일 인코딩: **UTF-8**
- 파일 끝: **빈 줄 1개**

### 중괄호

```java
// ✅ 올바른 예 — K&R 스타일
if (condition) {
doSomething();
} else {
doOther();
}

// ❌ 잘못된 예 — 단일 라인이라도 중괄호 생략 금지
        if (condition)
doSomething();
```

### 빈 줄

- 메서드 사이: 빈 줄 **1개**
- 클래스 내 논리적 섹션 구분: 빈 줄 **1개**
- 클래스 선언 직후 / 클래스 닫는 중괄호 직전: 빈 줄 **없음**

---

## 3. 네이밍 컨벤션

| 대상 | 규칙 | 예시 |
|---|---|---|
| 클래스 | UpperCamelCase | `UserService`, `OrderRepository` |
| 인터페이스 | UpperCamelCase | `PaymentGateway`, `UserReader` |
| 메서드 | lowerCamelCase (동사 시작) | `findById()`, `saveOrder()` |
| 변수 | lowerCamelCase | `userId`, `orderList` |
| 상수 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| 패키지 | 소문자, 단어 구분 없음 | `com.example.user` |
| Enum | UpperCamelCase / 값은 UPPER_SNAKE_CASE | `OrderStatus.PENDING` |

### 네이밍 상세 규칙

```java
// ✅ 의미 있는 이름 사용
List<User> activeUsers = userRepository.findAllByStatus(UserStatus.ACTIVE);

// ❌ 의미 없는 축약어 금지
List<User> lst = repo.find();

// ✅ Boolean 변수/메서드는 is/has/can 접두사
boolean isActive;
boolean hasPermission();

// ✅ 컬렉션 변수는 복수형
List<Order> orders;
Map<Long, User> userMap;
```

---

## 4. 패키지 및 임포트

### 패키지 구조

> 상세 구조는 `project-structure.md` 참고. 요약만 기재.

```
com.{company}.{project}
├── domain/
│   └── {도메인명}/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       └── dto/
│           ├── request/
│           └── response/
└── global/          # 공통 설정, 예외, 응답 포맷, 유틸
```

### 임포트

- **와일드카드 임포트 금지** (`import java.util.*` ❌)
- static import는 테스트 코드 또는 상수 참조에만 허용
- 미사용 임포트는 즉시 제거
- 완전 한정 이름(Fully Qualified Name, FQN) 방식은 사용하지 않는다.

```java
// ✅ 명시적 임포트
import java.util.List;
import java.util.Optional;

// ❌ 와일드카드 금지
import java.util.*;
```

---

## 5. 클래스 설계

### 클래스 내부 순서

```
1. static 상수 (static final)
2. 인스턴스 필드
3. 생성자
4. public 메서드
5. private 메서드
6. static 메서드
```

### Lombok 사용 규칙

```java
// ✅ 허용
@Getter
@RequiredArgsConstructor
@Builder

// ❌ 지양 — setter 남용은 불변성 훼손
@Setter
@Data  // equals/hashCode 자동 생성이 JPA Entity에서 문제 유발
```

### 인터페이스 설계

**사용 기준**

| 상황 | 판단 |
|---|---|
| 구현체가 2개 이상 존재하거나 예정 | ✅ 인터페이스 도입 |
| 테스트에서 Mock으로 대체해야 함 | ✅ 인터페이스 도입 |
| 구현체가 1개이고 교체 가능성 없음 | ❌ 인터페이스 불필요 — 구체 클래스 직접 사용 |
| "혹시 나중에 쓸 수도 있어서" | ❌ YAGNI — 도입하지 않음 |

**네이밍**

- `I` 접두사 금지 (`IUserService` ❌)
- 역할/능력 중심의 명사형으로 작성

```java
// ✅ 역할 중심
public interface MessageSender { ... }
public interface UserReader { ... }
public interface NotificationPort { ... }  // 외부 시스템 연동 포트

// ❌ 단순 클래스명 복사
public interface IUserService { ... }
public interface UserServiceInterface { ... }
```

**default 메서드**

- 기존 인터페이스에 메서드를 추가할 때 하위 호환성 유지 목적으로만 허용
- 공통 로직 재사용 수단으로 사용 금지 → abstract class 또는 별도 유틸로 분리

### 추상 클래스 설계

**사용 기준**

- 상태(필드)와 부분 구현을 함께 공유해야 할 때 사용
- 인터페이스만으로 공통 로직을 담을 수 없을 때 사용
- 대표 사례: JPA Entity 공통 필드(`createdAt`, `updatedAt`) 를 담는 base class

```java
// ✅ 공통 감사 필드 추상 클래스
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    private OffsetDateTime createdAt;

    @LastModifiedDate
    private OffsetDateTime updatedAt;
}

// ✅ 상속
@Entity
public class ChatRoom extends BaseTimeEntity { ... }
```

**네이밍**

- 직접 인스턴스화할 수 없는 기반 클래스임을 드러낼 때 `Abstract` 접두사 사용
- JPA base entity처럼 역할이 명확하면 `Base` 접두사도 허용

```java
public abstract class AbstractExternalApiClient { ... }  // 외부 API 클라이언트 공통 로직
public abstract class BaseTimeEntity { ... }             // JPA 감사 필드
```

**금지 사항**

- 인터페이스 대신 추상 클래스로 계약을 정의하는 것 금지 (Java 단일 상속 제약)
- 추상 클래스에 비즈니스 로직 집중 금지 — 공통 인프라 코드만 허용

---

## 6. 메서드 설계

- 메서드 길이: **30줄 이하** 권장
- 파라미터: **3개 이하** 권장 (초과 시 객체로 묶기)
- 반환 타입에 `null` 반환 금지 → `Optional<T>` 또는 빈 컬렉션 사용

```java
// ✅ Optional 활용
public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
}

// ✅ 빈 컬렉션 반환
public List<Order> findOrders(Long userId) {
    return orderRepository.findAllByUserId(userId); // 결과 없으면 빈 List
}

// ❌ null 반환 금지
public User getUser(Long id) {
    return null; // 절대 금지
}
```

---

## 7. Spring 레이어 규칙

### Controller

- URL은 **소문자 + kebab-case** 사용 (`/user-orders`)
- 응답은 `ResponseEntity<T>` 또는 공통 응답 래퍼 사용
- 비즈니스 로직을 Controller에 작성하지 않는다
- `@RequestBody` 파라미터는 반드시 `@Valid`로 검증

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUser(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateUserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        CreateUserResponse response = userService.createUser(request);
        URI location = URI.create("/api/v1/users/" + response.getId());
        return ResponseEntity.created(location).body(ApiResponse.ok(response));
    }
}
```

### Service

- `@Transactional` 은 Service 레이어에서만 사용
- 조회 전용 메서드는 `@Transactional(readOnly = true)` 필수
- 하나의 Service는 같은 도메인 내 Repository만 의존 (교차 도메인은 별도 Facade 또는 도메인 서비스)

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        User user = User.create(request.getEmail(), request.getName());
        return UserResponse.from(userRepository.save(user));
    }
}
```

### Repository

- Spring Data JPA 기본 메서드 활용 우선
- 복잡한 쿼리는 QueryDSL 또는 JPQL 사용 (`@Query`)
- Native Query는 최후 수단

```java
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.status = :status AND u.createdAt >= :from")
    List<User> findActiveUsersSince(@Param("status") UserStatus status,
                                   @Param("from") LocalDateTime from);
}
```

### DTO 규칙

- **Request / Response** DTO를 Entity와 분리
- Entity를 Controller 응답으로 직접 반환하지 않는다
- DTO 변환 로직은 DTO 내부 정적 팩토리 메서드로 처리

```java
// ✅ DTO 내부에서 변환
public record UserResponse(Long id, String email, String name) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName());
    }
}
```

---

## 8. 예외 처리

> 예외 구조, 응답 포맷, GlobalExceptionHandler 상세 정의는 `error-handling.md` 참고.

- 비즈니스 예외는 `BusinessException(ErrorCode)` 사용
- `try-catch`로 예외를 삼키지 않는다
- Controller에서 예외를 직접 catch하지 않음 → `GlobalExceptionHandler`에 위임
- `catch` 블록에서 예외 무시 금지 (`log.error` 후 반드시 rethrow 또는 변환)

```java
// ✅ 올바른 예
@Slf4j
@Service
public class OrderService {
    public void processOrder(Long orderId) {
        log.info("주문 처리 시작. orderId={}", orderId);
        try {
            // 처리 로직
        } catch (ExternalApiException e) {
            log.error("외부 API 호출 실패. orderId={}", orderId, e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }
}
```

---

## 9. 로깅

- 로거: **SLF4J + Logback** 사용
- `System.out.println` 사용 **금지**
- Lombok `@Slf4j` 어노테이션 사용

```java
@Slf4j
@Service
public class OrderService {

    public void processOrder(Long orderId) {
        log.info("주문 처리 시작. orderId={}", orderId);
        try {
            // 처리 로직
        } catch (Exception e) {
            log.error("주문 처리 실패. orderId={}", orderId, e);
            throw e;
        }
    }
}
```

### 로그 레벨 기준

| 레벨 | 사용 상황 |
|---|---|
| `ERROR` | 시스템 오류, 즉각적인 대응 필요 |
| `WARN` | 잠재적 문제, 비정상적이지만 처리 가능 |
| `INFO` | 중요 비즈니스 이벤트 (주문 생성, 결제 등) |
| `DEBUG` | 개발/디버깅용 상세 정보 (운영 환경 비활성화) |

---

## 10. 테스트 코드

> 테스트 계층, 파일 위치, 어노테이션 기준 상세는 `testing.md` 참고.

- 테스트 프레임워크: **JUnit 5 + AssertJ + Mockito**
- 테스트 클래스명: `{대상클래스}Test`
- 메서드명: 한글로 행위 중심 기술 (`존재하지_않는_유저_조회시_예외를_던진다`)
- **given / when / then** 구조 주석 필수
- 테스트에서 `@Autowired` 대신 생성자 주입 또는 `@InjectMocks` 사용

---

## 11. 주석 및 문서화

- 코드로 의도를 표현할 수 있으면 주석 **생략** 우선
- `// TODO:`, `// FIXME:` 는 담당자와 날짜 포함
- 공개 API에는 JavaDoc 작성

```java
/**
 * 이메일로 활성 사용자를 조회합니다.
 *
 * @param email 조회할 이메일 주소
 * @return 사용자 정보 (존재하지 않을 경우 empty)
 */
public Optional<User> findActiveUserByEmail(String email) { ... }

// TODO(홍길동, 2025-06-01): 페이지네이션 적용 필요
// FIXME(홍길동, 2025-05-27): 동시성 이슈 확인 후 수정
```

---

## 12. 금지 사항

| 항목 | 이유 |
|---|---|
| `System.out.println` | 로그 레벨 제어 불가 |
| Entity를 API 응답으로 직접 반환 | 불필요한 필드 노출, 순환 참조 위험 |
| Controller에 비즈니스 로직 작성 | 레이어 책임 혼합 |
| Service에서 다른 도메인 Repository 직접 의존 | 높은 결합도 |
| `@Autowired` 필드 주입 | 불변성 보장 불가, 테스트 어려움 → 생성자 주입 사용 |
| catch 블록에서 예외 무시 | 오류 원인 추적 불가 |
| `Optional.get()` 바로 호출 | `NoSuchElementException` 위험 |
| magic number / magic string | 의미 불명확 → 상수로 정의 |

---

> **개정 이력**
>
> | 버전 | 날짜 | 변경 내용 | 작성자 |
> |---|---|---|---|
> | 1.0.0 | YYYY-MM-DD | 최초 작성 | - |