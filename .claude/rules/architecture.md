
# 아키텍처 규칙

## 패키지 구조

도메인 중심으로 구성한다. 기능(controller, service, repository) 단위가 아니라 **도메인** 단위로 묶는다.

```
com.chat/
├── common/                  # 도메인 횡단 공통 모듈
└── {domain}/
    ├── api/                 # Controller
    ├── application/         # 비즈니스 인터페이스 + 구현체
    ├── domain/              # Entity, 도메인 모델
    ├── dto/                 # Request / Response DTO
    └── infrastructure/      # Repository, 외부 연동 구현체
```

`common`에는 특정 도메인에 속하지 않는 횡단 관심사만 위치시킨다.

## 의존성 방향

```
api → application → domain
              ↓
       infrastructure
```

- Controller는 인터페이스에만 의존한다. 구현체 직접 참조 금지.
- Controller에서 Repository 직접 호출 금지.

## 인터페이스·구현체 분리

`application` 레이어는 반드시 인터페이스와 구현체를 분리한다.

### 인터페이스 네이밍 — Spring 스타일, `Service` 접미사 지양

| 패턴 | 예시 | 적합한 경우 |
|------|------|------------|
| `-er` / `-or` | `UserInitializer` | 단일 행위 (초기화, 변환 등) |
| `Manager` | `ProfileManager` | CRUD 관리 |
| `Operations` | `RedisOperations` | 연산 집합 |

### 구현체 네이밍 — `Default` 접두사, `Impl` 접미사 지양

```java
// ✅
public interface UserInitializer { ... }
public class DefaultUserInitializer implements UserInitializer { ... }

public interface ProfileManager { ... }
public class DefaultProfileManager implements ProfileManager { ... }

// ❌
public interface UserService { ... }
public class UserServiceImpl implements UserService { ... }
```

## 레이어별 책임

| 레이어 | 책임 |
|--------|------|
| `api` | HTTP 요청 수신·응답 반환만. 비즈니스 로직 없음 |
| `application` | 비즈니스 로직, 트랜잭션 경계, Entity ↔ DTO 변환 |
| `domain` | Entity, 도메인 규칙. 상태 변경은 의미 있는 메서드로만 |
| `infrastructure` | JpaRepository, Redis, 외부 API 구현체 |
| `dto` | 레이어 간 데이터 전달. `record` 우선 사용 |
