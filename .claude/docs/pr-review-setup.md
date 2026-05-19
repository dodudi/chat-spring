# PR 자동 코드 리뷰 설정

PR이 열리면 GitHub Actions가 Discord로 알림을 보내고, 라즈베리파이의 Claude Code 에이전트가 자동으로 코드 리뷰를 수행한다.

---

## 동작 흐름

```
GitHub PR 오픈 / 업데이트 (synchronize)
    ↓
GitHub Actions (.github/workflows/pr-review.yml)
    ↓
Discord Bot API로 채널에 메시지 전송
    ↓
라즈베리파이 Claude Code 에이전트가 멘션 감지
    ↓
GitHub API로 PR diff 가져와서 리뷰 작성
    ↓
Discord 채널에 리뷰 결과 전송
```

---

## 구성 요소

### 1. GitHub Actions Workflow
- 파일: `.github/workflows/pr-review.yml`
- 트리거: PR `opened`, `synchronize`
- Discord Bot API (`/channels/{id}/messages`)로 메시지 전송

### 2. Discord Bot
- 메시지 전송 전용 봇 (오프라인 상태여도 동작)
- Discord REST API만 사용 — 게이트웨이 연결 불필요

### 3. 에이전트 멘션 ID
- `<@1505943257622909140>` (spring-java-reviewer)

### 4. 라즈베리파이 Claude Code 에이전트
- Discord 채널을 모니터링
- 멘션 메시지에서 PR URL 추출 후 리뷰 수행
- `server.ts`에서 봇 메시지 무시 로직 제거 필요 (이미 적용)

---

## GitHub Secrets 등록 항목

| Secret 이름 | 설명 |
|-------------|------|
| `DISCORD_BOT_TOKEN` | Discord Bot 토큰 |
| `DISCORD_CHANNEL_ID` | 메시지를 전송할 Discord 채널 ID |

등록 경로: `GitHub 저장소 → Settings → Secrets and variables → Actions`

---

## 트러블슈팅

### 에이전트가 메시지에 반응하지 않는 문제
- Webhook / Bot API 모두 `author.bot = true`로 처리됨
- Claude Code 에이전트가 봇 메시지를 기본적으로 무시 (봇 루프 방지 목적)
- **해결**: 라즈베리파이 `server.ts`에서 봇 ID를 무시하는 로직 제거

### Webhook → Bot API로 변경한 이유
- Discord Incoming Webhook은 단방향 (외부 → Discord 전송만 가능)
- Bot API는 채널 ID 기반으로 메시지 전송 가능
- 기능상 차이는 없으나 Bot API가 더 유연한 제어 가능
