# 로깅 규칙

## 로그 형식

비즈니스 이벤트 로그는 구조화된 태그를 포함한다.

```java
// ✅
log.info("[ROOM_CREATED] roomId={} userId={}", room.getId(), userId);
log.warn("[WS_AUTH_FAIL] token={}", token);
log.error("[REDIS_PUB_FAIL] roomId={}", roomId, e);

// ❌
log.info("채팅방이 생성됐습니다: " + room.getId());  // 태그 없음, 문자열 연산
```

## 로그 레벨 기준

| 레벨 | 기준 |
|------|------|
| `ERROR` | 복구 불가능한 장애 (DB 연결 실패, Redis pub 실패 등) |
| `WARN` | 예상 가능한 비즈니스 예외 (인증 실패, 리소스 없음) |
| `INFO` | 비즈니스 이벤트 (방 생성, 메시지 전송, WebSocket 연결) |
| `DEBUG` | 개발 디버깅 전용 — 운영 로그에 남기지 않는다 |

## MDC traceId

`RequestLoggingFilter`가 요청마다 자동으로 `traceId`를 MDC에 주입한다.
직접 `MDC.put("traceId", ...)`를 호출하지 않는다.
