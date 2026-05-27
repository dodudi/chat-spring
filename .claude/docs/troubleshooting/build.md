# 빌드 / 환경 트러블슈팅

---

## Spring Boot 4.0 Breaking Changes

**발생 시점**: v3 전환 초기

### @WebMvcTest 경로 변경

- **증상**: `@WebMvcTest` import가 기존 경로로 해결되지 않음
- **원인**: Spring Boot 4.0에서 패키지 경로 변경
- **해결**: 변경된 경로로 import 수정 (직접 확인 후 적용)

### @MockBean → @MockitoBean

- **증상**: `@MockBean` 사용 시 컴파일 경고 또는 오류
- **원인**: Spring Boot 4.0에서 `@MockBean` deprecated, `@MockitoBean`으로 대체
- **해결**: 테스트 코드 전체에서 `@MockBean` → `@MockitoBean` 교체

### ObjectMapper 동작 변경

- **증상**: 기존 직렬화/역직렬화 테스트 실패
- **원인**: Spring Boot 4.0에서 ObjectMapper 기본 설정 일부 변경
- **해결**: 직접 확인 후 필요한 설정 명시적으로 추가

---

## H2 + Flyway 호환성 문제

**발생 시점**: 로컬 환경 최초 구성

- **증상**: `application-local.yml`에서 H2 + Flyway 활성화 시 마이그레이션 스크립트 실패
- **원인**: PostgreSQL 전용 DDL 문법을 H2가 `MODE=PostgreSQL`에서도 일부 처리 못함
- **해결**: 로컬에서 Flyway 비활성화 + `ddl-auto: create-drop` 적용 → [ADR-002](../decisions/adr-002-local-dev-db.md) 참고
