# CLAUDE.md — chat-spring

Spring Boot 기반 채팅 서비스 백엔드 프로젝트입니다.

## 규칙 문서

코드 작성 전 반드시 아래 규칙 문서를 참고합니다.

| 문서 | 내용 |
|------|------|
| [code-style.md](.claude/rules/code-style.md) | Java/Spring 코드 스타일, 네이밍, 레이어 규칙 |
| [api-design.md](.claude/rules/api-design.md) | REST API URL 설계, 요청/응답 포맷, HTTP 메서드 기준 |
| [error-handling.md](.claude/rules/error-handling.md) | 공통 응답 포맷, 예외 구조, GlobalExceptionHandler |
| [project-structure.md](.claude/rules/project-structure.md) | 패키지 구조, 레이어 역할, 파일 네이밍 |
| [testing.md](.claude/rules/testing.md) | 테스트 종류별 작성 기준, given/when/then 구조 |
| [git-convention.md](.claude/rules/git-convention.md) | 브랜치 전략, 커밋 메시지 형식, PR 규칙 |

## 기획 문서

| 문서 | 내용 |
|------|------|
| [prd.md](.claude/docs/spec/prd.md) | 제품 요구사항, 핵심 기능, 사용자 시나리오 |
| [erd.md](.claude/docs/spec/erd.md) | 테이블 정의, 컬럼 타입/제약 조건, 관계 정의 |
| [api.md](.claude/docs/spec/api.md) | 전체 엔드포인트 목록, 요청/응답 상세 명세 |

## 아키텍처 결정 기록 (ADR)

| 문서 | 결정 내용 |
|------|----------|
| [ADR-001: 인증 전략](.claude/docs/decisions/adr-001-auth-strategy.md) | JWT 외부 인증 서버 위임, users 테이블 없음, JWT `sub` 클레임으로 사용자 식별 |
| [ADR-002: 로컬 개발 DB](.claude/docs/decisions/adr-002-local-dev-db.md) | H2 인메모리 + `MODE=PostgreSQL`, Flyway 비활성화, `ddl-auto: create-drop` |

## 트러블슈팅

| 문서 | 내용 |
|------|------|
| [build.md](.claude/docs/troubleshooting/build.md) | Spring Boot 4.0 breaking changes, H2+Flyway 호환성 문제 |
| [runtime.md](.claude/docs/troubleshooting/runtime.md) | 런타임 오류 및 해결 이력 |

## 변경 이력

| 문서 | 내용 |
|------|------|
| [api.md](.claude/docs/changelog/api.md) | API 엔드포인트 추가/변경/제거 이력 |
| [db.md](.claude/docs/changelog/db.md) | DB 스키마 변경 이력 |

## 문서 검증 규칙

문서(PRD·ERD·API) 검증 요청 시 반드시 아래 절차를 따른다.

### 검증 절차 (1회에 완료)

1. **세 문서를 동시에 전부 읽는다.** 부분만 읽거나 수정한 부분 주변만 보는 것 금지.
2. **PRD → ERD → API 순서로 교차 검증한다.**
   - PRD의 모든 기능 요구사항이 ERD 테이블/컬럼에 반영되었는가
   - ERD의 모든 테이블/컬럼/제약이 API 요청·응답·에러에 반영되었는가
   - API의 모든 에러 코드가 PRD 정책과 일치하는가
   - API의 모든 부가 처리(room_group_memberships 정리, Redis 키 삭제 등)가 빠짐없이 명시되었는가
   - WebSocket 이벤트 발행 조건이 각 HTTP API 동작과 일치하는가
3. **이슈를 한 번에 전부 취합한 뒤 일괄 수정한다.** 이슈를 발견할 때마다 부분 수정 후 재검증하는 방식 금지.
4. **수정 후 재검증은 최대 1회만 허용한다.** 검증 → 수정 → 재검증 이후에도 이슈가 나오면 절차가 잘못된 것이므로 처음부터 다시 수행한다.

### 제한
- **검증 + 수정 사이클은 총 3회를 초과할 수 없다.**
- 3회 안에 완료하지 못하면 이유를 사용자에게 보고하고 접근 방식을 바꾼다.

---

## 핵심 제약사항

- **인증**: 이 서버는 JWT를 검증만 하고 발급하지 않음. `users` 테이블 없음.
- **로컬 실행**: `local` 프로파일 사용 (`-Dspring.profiles.active=local`). H2 콘솔 `/h2-console`에서 확인 가능.
- **커밋/push**: 사용자가 명시적으로 요청할 때만 수행.
- **Spring Boot 버전**: 4.0 사용 중. `@MockBean` → `@MockitoBean`, `@WebMvcTest` 경로 변경 등 주의.
