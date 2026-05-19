# WebSocket / Redis 규칙

## STOMP 엔드포인트 및 경로

| 구분 | 값 |
|------|-----|
| 연결 엔드포인트 | `/ws/chat` |
| 앱 목적지 접두사 | `/app` |
| 브로커 접두사 | `/topic` (브로드캐스트), `/queue` (개인) |
| 사용자 목적지 접두사 | `/user` |

## 메시지 흐름

```
클라이언트 → /app/rooms/{roomId}/messages
    → StompChatHandler.sendMessage()
        → MessageService.sendMessage() (DB 저장)
            → ChatMessagePublisher.publish() (Redis pub)
                → ChatMessageSubscriber.onMessage() (Redis sub)
                    → /topic/rooms/{roomId} (브로드캐스트)
```

## Redis 채널 구조

| 채널 키 | 용도 |
|---------|------|
| `pubsub:room:{roomId}` | 채팅방 메시지 브로드캐스트 |
| `pubsub:user:{userId}` | 개인 알림 |
| `user:online:{userId}` | 온라인 상태 (TTL 60초) |

## 온라인 상태 처리

- STOMP CONNECT → `PresenceService.heartbeat()` → Redis TTL 키 설정
- STOMP DISCONNECT → `PresenceService.offline()` → Redis 키 삭제
- TTL: 60초. 연결 유지 시 주기적으로 갱신.

## JWT 인증

- `/ws/chat` 엔드포인트는 Spring Security에서 공개 경로로 설정
- 실제 인증은 STOMP CONNECT 프레임에서 `JwtChannelInterceptor`가 수행
- `Authorization: Bearer {token}` 헤더에서 JWT를 추출해 `JwtPrincipal`을 설정

## WebSocket 메시지 매핑

```java
@MessageMapping("/rooms/{roomId}/messages")
public void sendMessage(@DestinationVariable Long roomId,
                        @Valid @Payload SendMessageRequest request,
                        Principal principal) { ... }
```

- `Principal.getName()`으로 userId를 추출한다.
- `@Valid`로 Payload를 검증한다.
