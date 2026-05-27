# DB 스키마 변경 이력

---

<!-- 형식
## [vX.Y / VYYYYMMdd__설명.sql] YYYY-MM-DD

- **변경 내용**: 추가/수정/삭제된 테이블, 컬럼
- **이유**: 왜 이 변경이 필요했는지
- **영향 범위**: 영향받는 API, 서비스 레이어
-->

## [v3] 설계 중

- users 테이블 없음 — JWT sub 클레임으로 사용자 식별 ([ADR-001](../decisions/adr-001-auth-strategy.md) 참고)
- 스키마 확정 시 Flyway 마이그레이션 파일과 함께 기록 예정
