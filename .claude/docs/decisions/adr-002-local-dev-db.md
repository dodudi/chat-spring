# ADR-002: 로컬 개발 DB — H2 인메모리 + create-drop

- **날짜**: 2025-05-27
- **상태**: 승인됨

## 배경

로컬 개발 환경에서 PostgreSQL을 직접 띄우거나 Docker를 강제하면 진입 장벽이 높아짐.
Flyway + H2 조합 시 PostgreSQL 전용 DDL 문법(예: `TEXT`, 시퀀스 등)이 H2에서 실패하는 문제 발생.

## 결정

- 로컬 프로파일(`application-local.yml`)에서 **H2 인메모리 DB** 사용
- H2 URL에 `MODE=PostgreSQL` 옵션 적용하여 문법 호환성 확보
- **Flyway 비활성화** (`spring.flyway.enabled: false`)
- **JPA ddl-auto: create-drop** 으로 스키마 자동 생성
- 운영/개발 서버에서는 PostgreSQL + Flyway 정상 사용

## 결과

- 장점: Docker 없이 바로 실행 가능, 빠른 개발 사이클
- 단점: Flyway 마이그레이션 검증은 로컬에서 불가 → CI에서 PostgreSQL 컨테이너로 검증 필요
- H2 `MODE=PostgreSQL`로도 커버 안 되는 문법은 별도 확인 필요
