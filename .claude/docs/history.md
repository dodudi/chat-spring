# 구현 현황

spec 요구사항 대비 현재 구현 상태. 작업 후 반드시 이 파일을 최신 상태로 유지한다.

---

## R — 채팅방

| ID | 요구사항 | 상태 |
|----|---------|------|
| R-01 | DM 방 생성 또는 기존 DM 방 재사용 (find-or-create, 멱등) | ✅ |
| R-02 | 그룹 채팅방 생성 (2명 이상) | ✅ |
| R-03 | 내 채팅방 목록 조회 (최근 메시지 순 정렬) | ✅ |
| R-04 | 채팅방 나가기 (그룹: 퇴장, DM: 숨김 처리) | ✅ |
| R-05 | 그룹 채팅방 멤버 초대 | ✅ |
| R-06 | 채팅방별 읽지 않은 메시지 수 표시 | ✅ R-03 응답에 포함 |

---

## M — 메시지

| ID | 요구사항 | 상태 |
|----|---------|------|
| M-01 | 채팅방에 텍스트 메시지 전송 | ✅ |
| M-02 | 메시지 히스토리 커서 기반 페이징 조회 | ✅ |
| M-03 | 본인 메시지 삭제 (soft delete) | ✅ |
| M-04 | 방 목록에 마지막 메시지·시간 표시 | ✅ R-03 응답에 포함 |
| M-05 | 파일/이미지 첨부 | ⏸ 미결정 — 직접 처리 vs S3 pre-signed URL |

---

## S — 실시간

| ID | 요구사항 | 상태 |
|----|---------|------|
| S-01 | 채팅방 실시간 메시지 수신 | ✅ Redis Pub/Sub → `/topic/rooms/{roomId}` |
| S-02 | 사용자 온라인 상태 표시 (heartbeat + 조회) | ✅ heartbeat TTL 60s / `GET /api/v1/users/{userId}/presence` |
| S-02 | WebSocket disconnect 시 즉시 오프라인 처리 | ✅ SessionDisconnectEvent → `PresenceService.offline()` |
| S-03 | 개인 알림 채널 수신 (초대 알림) | ✅ `/user/queue/notifications` |

---

---

## D — 배포 인프라

| ID | 항목 | 상태 | 비고 |
|----|------|------|------|
| D-01 | Docker 이미지 빌드 / Docker Compose 배포 | ✅ | `Dockerfile`, `docker-compose.yml` |
| D-02 | GitHub Actions 자동 배포 (Docker Hub push) | ✅ | `.github/workflows/docker-deploy.yml` |
| D-03 | nginx 리버스 프록시 (HTTPS + WebSocket) | ✅ | `proxy_http_version 1.1` / Upgrade 헤더 설정 |
| D-04 | 운영 도메인 `chat.rudy.it.kr` 연결 | ✅ | Raspberry Pi 배포 완료 |
| D-05 | CORS 설정 (`CorsConfigurationSource` 빈) | ✅ | `CORS_ALLOWED_ORIGINS` 환경변수로 제어 |

---

## B — 버그 수정

| ID | 항목 | 상태 | 비고 |
|----|------|------|------|
| B-01 | `RoomSummaryProjection` Instant/OffsetDateTime 타입 불일치 | ✅ | PostgreSQL `TIMESTAMPTZ` → Hibernate `Instant` 반환 문제. projection 타입 변경 + `toOffsetDateTime()` 헬퍼 추가 |

---

## T — 테스트 클라이언트

| ID | 항목 | 상태 | 비고 |
|----|------|------|------|
| T-01 | 로컬 STOMP 테스트 클라이언트 | ✅ | `test-client/stomp-test.html` — H2 로컬 서버용 |
| T-02 | 운영 STOMP 테스트 클라이언트 (client_credentials) | ✅ | `test-client/stomp-test-prod.html` — `client_credentials` 방식 토큰 발급 |
| T-03 | 운영 STOMP 테스트 클라이언트 (Auth Code + PKCE) | ✅ | `test-client/stomp-test-prod-login.html` — 팝업 로그인 / 폼 submit + postMessage 방식 |
| T-04 | 채팅방 생성 (DM / 그룹) REST 기능 | ✅ | T-01, T-02, T-03 모두 포함 |

> 테스트 클라이언트는 `.gitignore` 대상 (`test-client/`) — 저장소에 포함되지 않음.

---

## 범례

| 기호 | 의미 |
|------|------|
| ✅ | 구현 완료 |
| ❌ | 미구현 |
| ⏸ | 보류 (의사결정 필요) |
| 🔧 | 진행 중 |
