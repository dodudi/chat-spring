# 디렉토리 구조 규칙

이 파일은 패키지 및 디렉토리 구조 작성 시 항상 따라야 할 규칙을 정의한다.

---

## 원칙 — 기능적 응집도 우선

패키지는 기술적 역할이 아닌 기능 도메인을 기준으로 나눈다.
"이 파일을 수정할 때 함께 수정하게 될 파일들이 같은 패키지에 있는가?"를 기준으로 판단한다.

```
// ✅ 올바른 예 — 도메인별로 설정 포함
com.auth.security.config.SecurityConfig
com.auth.admin.config.AdminSecurityConfig

// ❌ 잘못된 예 — 기술적 역할 버킷
com.auth.config.SecurityConfig
com.auth.config.AdminSecurityConfig
```

---

## 전체 패키지 구조 (예시)

```
com.example/
├── ExampleApplication.java
│
├── common/                        도메인에 속하지 않는 공유 코드
│   ├── config/                    인프라 공통 설정 (JPA Auditing 등)
│   ├── exception/                 CustomException, ErrorCode, GlobalExceptionHandler
│   ├── filter/                    RequestLoggingFilter
│   ├── response/                  ApiResponse
│   └── util/                      정적 유틸 클래스
│
├── order/                         주문 도메인
│   ├── domain/                    Entity, Repository, Enum
│   ├── application/               Service (비즈니스 로직)
│   ├── web/                       @Controller — Thymeleaf 뷰 반환
│   ├── api/                       @RestController — JSON 응답
│   └── dto/                       Request / Response 레코드
│
├── payment/                       결제 도메인
│   ├── config/                    도메인 전용 설정
│   ├── domain/                    Repository 인터페이스
│   │   └── support/               Repository 커스텀 구현체
│   ├── application/               Service 인터페이스 + 구현체
│   ├── web/                       @Controller — Thymeleaf 뷰 반환
│   ├── api/                       @RestController — JSON 응답
│   └── dto/                       Request / Response 레코드
│
└── notification/                  알림 도메인
    ├── config/                    도메인 전용 설정
    ├── property/                  외부 설정값 바인딩 클래스
    ├── application/               Service
    ├── handler/                   이벤트 핸들러
    ├── web/                       @Controller — Thymeleaf 뷰 반환
    └── api/                       @RestController — JSON 응답
```

---

## 도메인 내부 서브패키지 규칙

| 서브패키지 | 포함 대상 | 비고 |
|-----------|----------|------|
| `domain/` | Entity, Repository 인터페이스, Enum | JPA 영속성 경계 |
| `domain/support/` | Repository 커스텀 구현체 | 인터페이스와 분리 (Spring 관례) |
| `application/` | Service 인터페이스·구현체 | 비즈니스 로직 |
| `web/` | `@Controller` | Thymeleaf 뷰 반환 |
| `api/` | `@RestController` | JSON 응답 |
| `dto/` | Request·Response 레코드 | 레이어 간 데이터 전달 |
| `config/` | `@Configuration` 클래스 | 해당 도메인 설정만 포함 |
| `property/` | `@ConfigurationProperties` 클래스 | 외부 설정값 바인딩 |
| `handler/` | 이벤트·예외 핸들러 | |

---

## 리소스 디렉토리 구조

```
src/main/resources/
├── application.yaml               공통 설정
├── application-local.yaml         로컬 프로파일
├── application-prod.yaml          운영 프로파일
└── templates/                     Thymeleaf 템플릿 (패키지 구조와 대응)
    ├── common/                    공통 레이아웃, 프래그먼트
    └── {domain}/                  도메인별 뷰 (예: admin/user/list.html)
```

`web/` 패키지의 컨트롤러가 반환하는 뷰 이름은 `templates/` 하위 경로와 일치시킨다.

```java
// web/UserController.java
return "admin/user/list";   // → templates/admin/user/list.html
```

---

## 설정 클래스 위치 규칙

설정 클래스는 중앙 `config/` 패키지에 모으지 않고 해당 도메인 하위 `config/`에 둔다.
특정 도메인에 속하지 않는 인프라 설정(JPA Auditing 등)은 `common/config/`에 둔다.

---

## 인터페이스·구현체 위치 규칙

Repository 인터페이스는 `domain/`에, 커스텀 구현체는 `domain/support/`에 둔다.

```
payment/domain/
├── OrderRepository.java           ← 인터페이스
└── support/
    └── OrderRepositoryImpl.java   ← 커스텀 구현체
```

Service 인터페이스와 구현체는 같은 `application/` 패키지에 둔다.

```
payment/application/
├── PaymentService.java            ← 인터페이스
└── SimplePaymentService.java      ← 구현체
```

---

## 금지 사항

```
// ❌ impl/ 패키지 사용 금지
com.example.payment.application.impl.SimplePaymentService

// ❌ 중앙 config/ 패키지 금지
com.example.config.SecurityConfig
com.example.config.PaymentConfig

// ❌ 기술적 분류 패키지 금지
com.example.interfaces/
com.example.services/
com.example.repositories/

// ❌ api/에 @Controller 혼용 금지, web/에 @RestController 혼용 금지
com.example.order.api.OrderViewController      // @Controller를 api/에 배치
com.example.order.web.OrderRestController      // @RestController를 web/에 배치
```

---

## 새 도메인 추가 시 체크리스트

1. 루트 하위에 도메인명 패키지 생성 (`com.example.{domain}/`)
2. 필요한 서브패키지만 생성 — 비어 있는 패키지는 만들지 않는다
3. 도메인 전용 설정이 있으면 `{domain}/config/`에 배치
4. 다른 도메인에서 참조하는 공유 코드는 `common/`으로 이동
