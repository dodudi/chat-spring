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

## 핵심 제약사항

- **인증**: 이 서버는 JWT를 검증만 하고 발급하지 않음. `users` 테이블 없음.
- **로컬 실행**: `local` 프로파일 사용 (`-Dspring.profiles.active=local`). H2 콘솔 `/h2-console`에서 확인 가능.
- **커밋/push**: 사용자가 명시적으로 요청할 때만 수행.
- **Spring Boot 버전**: 4.0 사용 중. `@MockBean` → `@MockitoBean`, `@WebMvcTest` 경로 변경 등 주의.
