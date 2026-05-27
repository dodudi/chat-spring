# Project Structure

## 패키지 구조

```
src/
└── main/
    ├── java/com/{company}/{project}/
    │   ├── domain/                     # 도메인별 패키지 (핵심)
    │   │   └── {domain}/
    │   │       ├── controller/         # REST 컨트롤러
    │   │       ├── service/            # 비즈니스 로직
    │   │       ├── repository/         # JPA Repository 인터페이스
    │   │       ├── entity/             # JPA Entity
    │   │       └── dto/                # 요청/응답 DTO
    │   │           ├── request/
    │   │           └── response/
    │   ├── global/                     # 전역 공통
    │   │   ├── config/                 # 설정 클래스 (Security, JPA 등)
    │   │   ├── exception/              # 공통 예외 클래스
    │   │   ├── response/               # 공통 응답 포맷
    │   │   └── util/                   # 유틸리티 클래스
    │   └── {ProjectName}Application.java
    └── resources/
        ├── application.yml
        ├── application-local.yml
        ├── application-dev.yml
        └── application-prod.yml
```

## 레이어별 역할

| 레이어 | 역할 | 규칙 |
|--------|------|------|
| Controller | HTTP 요청/응답 처리 | 비즈니스 로직 금지, DTO 변환만 |
| Service | 비즈니스 로직 | Entity 직접 반환 금지, DTO로 변환 |
| Repository | DB 접근 | JpaRepository 상속, 복잡한 쿼리는 JPQL/QueryDSL |
| Entity | DB 테이블 매핑 | 비즈니스 로직 최소화, setter 금지 |
| DTO | 데이터 전달 | Request/Response 분리 |

## 파일 네이밍 규칙

```
# Controller
UserController.java

# Service
# 구현체가 1개이고 교체 가능성 없음 → 구체 클래스만
ChatService.java

# 구현체가 2개 이상이거나 테스트에서 Mock 필요 → 인터페이스 + 구현체
# 인터페이스: 역할 중심 명사형 (I 접두사 금지)
# 구현체: 기본 구현은 Default 접두사, 대체 구현은 기술/방식 접두사
ChatService.java             # 인터페이스
DefaultChatService.java      # 기본 구현체
RedisChatService.java        # 대체 구현체 (예: 저장소 변경 시)

# 인터페이스 도입 기준 상세 → code-style.md 인터페이스 설계 참고

# Repository
UserRepository.java

# Entity
User.java                    # 단수형

# DTO
CreateUserRequest.java       # 동사 + 도메인 + Request
UserResponse.java            # 도메인 + Response
UserListResponse.java        # 목록은 List 접두어
```

## 의존성 방향

```
Controller → Service → Repository → Entity
```
- 상위 레이어가 하위 레이어를 의존 (역방향 금지)
- Controller는 Repository를 직접 참조하지 않음

## 설정 파일 분리 원칙

- `application.yml` — 공통 설정만 (프로파일 무관한 것)
- `application-local.yml` — 로컬 DB, 디버그 설정
- `application-dev.yml` — 개발 서버 설정
- `application-prod.yml` — 운영 서버 설정 (시크릿은 환경변수로)
- 민감 정보(DB 비밀번호, API Key)는 절대 yml에 하드코딩하지 않음