# 아키텍처 규칙

## 패키지 구조

도메인 중심으로 구성한다. 기능(controller, service, repository) 단위가 아니라 **도메인** 단위로 묶는다.

```
com.chat/
├── common/                  # 도메인 횡단 공통 모듈
└── {domain}/
    ├── api/                 # Controller
    ├── application/         # Service 인터페이스 + 구현체
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

- Controller는 Service 인터페이스에만 의존한다. `ServiceImpl` 직접 참조 금지.
- Controller에서 Repository 직접 호출 금지.

## Service 인터페이스 분리

Service는 반드시 인터페이스(`{Domain}Service`)와 구현체(`{Domain}ServiceImpl`)를 분리한다.

```java
// ✅
public interface RoomService { ... }

@Service
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService { ... }

// ❌ 인터페이스 없이 구현체만 존재
@Service
public class RoomService { ... }
```

## 레이어별 책임

| 레이어 | 책임 |
|--------|------|
| `api` | HTTP 요청 수신·응답 반환만. 비즈니스 로직 없음 |
| `application` | 비즈니스 로직, 트랜잭션 경계, Entity ↔ DTO 변환 |
| `domain` | Entity, 도메인 규칙. 상태 변경은 의미 있는 메서드로만 |
| `infrastructure` | JpaRepository, Redis, 외부 API 구현체 |
| `dto` | 레이어 간 데이터 전달. `record` 우선 사용 |
