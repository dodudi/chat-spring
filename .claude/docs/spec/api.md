# API 명세

## 공통

### 인증
모든 API는 `Authorization: Bearer {JWT}` 헤더 필수.

### 응답 포맷
```json
// 성공
{ "success": true, "data": { ... } }

// 실패
{ "success": false, "error": { "code": "ERROR_CODE", "message": "..." } }
```

---

## 1. 프로필

### GET /api/v1/profiles/me
내 프로필 조회.

**응답 200**
```json
{
  "success": true,
  "data": {
    "userId": "user-abc",
    "nickname": "홍길동",
    "createdAt": "2024-01-15T09:30:00Z",
    "updatedAt": "2024-01-15T09:30:00Z"
  }
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `PROFILE_NOT_FOUND` | 프로필 미등록 상태 |

---

### PUT /api/v1/profiles/me
내 프로필 등록 또는 수정. 없으면 생성, 있으면 수정.

**요청**
```json
{
  "nickname": "홍길동"
}
```

| 필드 | 타입 | 필수 | 검증 |
|------|------|------|------|
| `nickname` | String | Y | 1~50자 |

**응답 200**
```json
{
  "success": true,
  "data": {
    "userId": "user-abc",
    "nickname": "홍길동",
    "createdAt": "2024-01-15T09:30:00Z",
    "updatedAt": "2024-01-15T09:30:00Z"
  }
}
```

---

### GET /api/v1/profiles/{userId}
특정 사용자 프로필 조회.

**Path Variable**
| 파라미터 | 설명 |
|----------|------|
| `userId` | JWT sub 값 |

**응답 200**
```json
{
  "success": true,
  "data": {
    "userId": "user-xyz",
    "nickname": "김철수"
  }
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `PROFILE_NOT_FOUND` | 존재하지 않는 사용자 |

---

## 2. 채팅방

### GET /api/v1/chat-rooms
내가 참여 중인 채팅방 목록 조회. (숨김 처리된 방 제외)

**응답 200**
```json
{
  "success": true,
  "data": [
    {
      "roomId": "550e8400-e29b-41d4-a716-446655440000",
      "type": "DM",
      "name": "김철수",
      "role": "MEMBER",
      "joinedAt": "2024-01-15T09:30:00Z"
    },
    {
      "roomId": "661f9511-f30c-52e5-b827-557766551111",
      "type": "GROUP",
      "name": "개발팀",
      "role": "OWNER",
      "joinedAt": "2024-01-10T08:00:00Z"
    }
  ]
}
```

---

### GET /api/v1/chat-rooms/{roomId}
채팅방 단건 조회. 멤버만 조회 가능.

**응답 200**
```json
{
  "success": true,
  "data": {
    "roomId": "550e8400-e29b-41d4-a716-446655440000",
    "type": "GROUP",
    "name": "개발팀",
    "memberCount": 5,
    "createdAt": "2024-01-10T08:00:00Z"
  }
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `CHAT_ROOM_ACCESS_DENIED` | 멤버가 아닌 경우 |

---

### DELETE /api/v1/chat-rooms/{roomId}
채팅방 삭제. OWNER 전용.

**응답 204**

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `MEMBER_PERMISSION_DENIED` | OWNER가 아닌 경우 |

---

### POST /api/v1/chat-rooms/dm
DM 채팅방 생성. 이미 존재하면 기존 방 반환.

**요청**
```json
{
  "targetUserId": "user-xyz"
}
```

| 필드 | 타입 | 필수 | 검증 |
|------|------|------|------|
| `targetUserId` | String | Y | 본인 ID 불가 |

**응답 200** (기존 방 반환) / **201** (신규 생성)
```json
{
  "success": true,
  "data": {
    "roomId": "550e8400-e29b-41d4-a716-446655440000",
    "type": "DM"
  }
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `PROFILE_NOT_FOUND` | 상대방 프로필 없음 |

---

### POST /api/v1/chat-rooms/groups
GROUP 채팅방 생성. 생성자는 자동으로 OWNER.

**요청**
```json
{
  "name": "개발팀"
}
```

| 필드 | 타입 | 필수 | 검증 |
|------|------|------|------|
| `name` | String | Y | 1~100자 |

**응답 201**
```json
{
  "success": true,
  "data": {
    "roomId": "661f9511-f30c-52e5-b827-557766551111",
    "type": "GROUP",
    "name": "개발팀"
  }
}
```

---

### POST /api/v1/chat-rooms/public
PUBLIC 채널 생성. 생성자는 자동으로 OWNER.

**요청**
```json
{
  "roomKey": "dev-general",
  "name": "개발 일반"
}
```

| 필드 | 타입 | 필수 | 검증 |
|------|------|------|------|
| `roomKey` | String | Y | 1~100자, 영문·숫자·하이픈만 허용 |
| `name` | String | Y | 1~100자 |

**응답 201**
```json
{
  "success": true,
  "data": {
    "roomId": "772g0622-g41d-63f6-c938-668877662222",
    "type": "PUBLIC",
    "roomKey": "dev-general",
    "name": "개발 일반"
  }
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_ALREADY_EXISTS` | room_key 중복 |

---

### GET /api/v1/chat-rooms/public
PUBLIC 채널 목록 조회. (페이지네이션)

**쿼리 파라미터**
| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `cursor` | String | — | 마지막 조회 roomId (없으면 처음부터) |
| `size` | Int | 20 | 조회 개수 |
| `keyword` | String | — | 채널명 검색 |

**응답 200**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "roomId": "772g0622-g41d-63f6-c938-668877662222",
        "roomKey": "dev-general",
        "name": "개발 일반",
        "memberCount": 42
      }
    ],
    "nextCursor": "772g0622-...",
    "hasNext": true
  }
}
```

---

### POST /api/v1/chat-rooms/public/{roomKey}/join
PUBLIC 채널 참여.

**응답 200**
```json
{
  "success": true,
  "data": {
    "roomId": "772g0622-g41d-63f6-c938-668877662222",
    "type": "PUBLIC",
    "name": "개발 일반"
  }
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채널 |
| `MEMBER_ALREADY_EXISTS` | 이미 참여 중 |

---

## 3. 멤버 관리

### GET /api/v1/chat-rooms/{roomId}/members
채팅방 활성 멤버 목록 조회. (나간 멤버 제외)

**응답 200**
```json
{
  "success": true,
  "data": [
    {
      "userId": "user-abc",
      "nickname": "홍길동",
      "role": "OWNER",
      "joinedAt": "2024-01-10T08:00:00Z"
    },
    {
      "userId": "user-xyz",
      "nickname": "김철수",
      "role": "MEMBER",
      "joinedAt": "2024-01-11T09:00:00Z"
    }
  ]
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `CHAT_ROOM_ACCESS_DENIED` | 멤버가 아닌 경우 |

---

### PATCH /api/v1/chat-rooms/{roomId}/members/{userId}/role
멤버 역할 변경. OWNER 전용.

**요청**
```json
{
  "role": "ADMIN"
}
```

| 필드 | 타입 | 필수 | 검증 |
|------|------|------|------|
| `role` | String | Y | `ADMIN` / `MEMBER` |

**응답 204**

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `MEMBER_NOT_FOUND` | 대상 멤버 없음 |
| `MEMBER_PERMISSION_DENIED` | OWNER가 아닌 경우 |

---

### DELETE /api/v1/chat-rooms/{roomId}/members/me
채팅방 나가기. `left_at` 기록.

**응답 204**

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `MEMBER_NOT_FOUND` | 이미 나간 상태 |

---

### PATCH /api/v1/chat-rooms/{roomId}/members/me/hidden
채팅방 나만 숨기기. 상대방에게는 유지, 내 목록에서만 제거. `is_hidden = true` 기록.

**응답 204**

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `MEMBER_NOT_FOUND` | 멤버가 아닌 경우 |

---

### DELETE /api/v1/chat-rooms/{roomId}/members/{userId}
멤버 강제 추방. OWNER / ADMIN 전용. `kicked_at` 기록.

**응답 204**

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `MEMBER_NOT_FOUND` | 대상 멤버 없음 |
| `MEMBER_PERMISSION_DENIED` | OWNER/ADMIN이 아닌 경우 |

---

## 4. 초대

### POST /api/v1/chat-rooms/{roomId}/invitations
특정 사용자에게 초대장 발송. 멤버 누구나 가능.

**요청**
```json
{
  "inviteeId": "user-xyz"
}
```

| 필드 | 타입 | 필수 | 검증 |
|------|------|------|------|
| `inviteeId` | String | Y | 이미 멤버인 사용자 불가 |

**응답 201**
```json
{
  "success": true,
  "data": {
    "invitationId": 1,
    "roomId": "550e8400-e29b-41d4-a716-446655440000",
    "inviteeId": "user-xyz",
    "status": "PENDING",
    "expiresAt": "2024-01-22T09:30:00Z",
    "createdAt": "2024-01-15T09:30:00Z"
  }
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `CHAT_ROOM_ACCESS_DENIED` | 멤버가 아닌 경우 |
| `MEMBER_ALREADY_EXISTS` | 대상이 이미 멤버 |
| `INVITATION_ALREADY_PROCESSED` | 이미 PENDING 초대 존재 |

---

### GET /api/v1/invitations
내게 온 PENDING 초대 목록 조회.

**응답 200**
```json
{
  "success": true,
  "data": [
    {
      "invitationId": 1,
      "roomId": "550e8400-e29b-41d4-a716-446655440000",
      "roomName": "개발팀",
      "inviterId": "user-abc",
      "inviterNickname": "홍길동",
      "expiresAt": "2024-01-22T09:30:00Z",
      "createdAt": "2024-01-15T09:30:00Z"
    }
  ]
}
```

---

### PATCH /api/v1/invitations/{invitationId}
초대 수락 또는 거절.

**요청**
```json
{
  "status": "ACCEPTED"
}
```

| 필드 | 타입 | 필수 | 검증 |
|------|------|------|------|
| `status` | String | Y | `ACCEPTED` / `REJECTED` |

**응답 200**
```json
{
  "success": true,
  "data": {
    "invitationId": 1,
    "status": "ACCEPTED"
  }
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `INVITATION_NOT_FOUND` | 존재하지 않는 초대 |
| `INVITATION_EXPIRED` | 만료된 초대 |
| `INVITATION_ALREADY_PROCESSED` | 이미 수락/거절된 초대 |

---

### POST /api/v1/chat-rooms/{roomId}/invite-links
초대 링크 생성. 멤버 누구나 가능.

**요청**
```json
{
  "expiresAt": "2024-01-22T09:30:00Z"
}
```

| 필드 | 타입 | 필수 | 검증 |
|------|------|------|------|
| `expiresAt` | OffsetDateTime | Y | 현재 시각 이후 |

**응답 201**
```json
{
  "success": true,
  "data": {
    "linkId": 1,
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "inviteUrl": "/api/v1/invite-links/550e8400-e29b-41d4-a716-446655440000/join",
    "expiresAt": "2024-01-22T09:30:00Z",
    "isActive": true
  }
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `CHAT_ROOM_ACCESS_DENIED` | 멤버가 아닌 경우 |

---

### GET /api/v1/chat-rooms/{roomId}/invite-links
채팅방의 활성 초대 링크 목록 조회. 멤버 누구나 가능.

**응답 200**
```json
{
  "success": true,
  "data": [
    {
      "linkId": 1,
      "token": "550e8400-e29b-41d4-a716-446655440000",
      "inviteUrl": "/api/v1/invite-links/550e8400-e29b-41d4-a716-446655440000/join",
      "createdBy": "user-abc",
      "expiresAt": "2024-01-22T09:30:00Z",
      "isActive": true
    }
  ]
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `CHAT_ROOM_ACCESS_DENIED` | 멤버가 아닌 경우 |

---

### PATCH /api/v1/chat-rooms/{roomId}/invite-links/{linkId}/deactivate
초대 링크 비활성화. `is_active = false` 처리.

**응답 204**

**에러**
| 코드 | 상황 |
|------|------|
| `INVITE_LINK_NOT_FOUND` | 존재하지 않는 링크 |
| `CHAT_ROOM_ACCESS_DENIED` | 멤버가 아닌 경우 |

---

### POST /api/v1/invite-links/{token}/join
초대 링크 토큰으로 채팅방 참여.

**응답 200**
```json
{
  "success": true,
  "data": {
    "roomId": "550e8400-e29b-41d4-a716-446655440000",
    "type": "GROUP",
    "name": "개발팀"
  }
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `INVITE_LINK_NOT_FOUND` | 존재하지 않는 토큰 |
| `INVITE_LINK_EXPIRED` | 만료되었거나 비활성화된 링크 |
| `MEMBER_ALREADY_EXISTS` | 이미 참여 중 |

---

## 5. 메시지

### POST /api/v1/chat-rooms/{roomId}/messages
메시지 전송. WebSocket 연결이 없을 때 HTTP fallback으로도 사용 가능.

**요청**
```json
{
  "content": "안녕하세요!",
  "messageType": "TEXT"
}
```

| 필드 | 타입 | 필수 | 검증 |
|------|------|------|------|
| `content` | String | Y | 1~4000자 |
| `messageType` | String | N | `TEXT` (기본값). `IMAGE` / `FILE` 은 추후 지원 |

**응답 201**
```json
{
  "success": true,
  "data": {
    "messageId": 1704067201001,
    "roomId": "550e8400-e29b-41d4-a716-446655440000",
    "senderId": "user-abc",
    "senderNickname": "홍길동",
    "content": "안녕하세요!",
    "messageType": "TEXT",
    "createdAt": "2024-01-15T09:30:00Z"
  }
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `CHAT_ROOM_ACCESS_DENIED` | 멤버가 아닌 경우 |

---

### GET /api/v1/chat-rooms/{roomId}/messages
메시지 목록 조회. 최신 메시지부터 과거 방향 커서 기반 페이지네이션.

**쿼리 파라미터**
| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `before` | Long | — | 이 messageId(Snowflake)보다 오래된 메시지 조회. 없으면 최신부터 |
| `size` | Int | 50 | 조회 개수 |

**응답 200**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "messageId": 1704067202005,
        "senderId": "user-abc",
        "senderNickname": "홍길동",
        "content": "안녕하세요!",
        "messageType": "TEXT",
        "createdAt": "2024-01-15T09:30:02Z",
        "deleted": false
      },
      {
        "messageId": 1704067201001,
        "senderId": "user-xyz",
        "senderNickname": "김철수",
        "content": null,
        "messageType": "TEXT",
        "createdAt": "2024-01-15T09:30:01Z",
        "deleted": true
      }
    ],
    "nextCursor": 1704067201001,
    "hasNext": true
  }
}
```

> 철회된 메시지는 `deleted: true`, `content: null` 로 반환.

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `CHAT_ROOM_ACCESS_DENIED` | 멤버가 아닌 경우 |

---

### DELETE /api/v1/chat-rooms/{roomId}/messages/{messageId}
메시지 철회. 본인 메시지만 가능. `deleted_at` 소프트 삭제.

**응답 204**

**에러**
| 코드 | 상황 |
|------|------|
| `MESSAGE_NOT_FOUND` | 존재하지 않는 메시지 |
| `MESSAGE_DELETE_FORBIDDEN` | 본인 메시지가 아닌 경우 |

---

### PUT /api/v1/chat-rooms/{roomId}/read
읽음 처리. 마지막으로 읽은 messageId를 Redis에 갱신.

**요청**
```json
{
  "lastReadMessageId": 1704067202005
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `lastReadMessageId` | Long | Y | 마지막으로 읽은 Snowflake ID |

**응답 204**

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `CHAT_ROOM_ACCESS_DENIED` | 멤버가 아닌 경우 |

---

## 6. 그룹

### GET /api/v1/user-groups
내 그룹 목록 조회.

**응답 200**
```json
{
  "success": true,
  "data": [
    {
      "groupId": 1,
      "name": "업무",
      "roomCount": 3,
      "createdAt": "2024-01-10T08:00:00Z"
    },
    {
      "groupId": 2,
      "name": "친구",
      "roomCount": 5,
      "createdAt": "2024-01-11T08:00:00Z"
    }
  ]
}
```

---

### POST /api/v1/user-groups
그룹 생성.

**요청**
```json
{
  "name": "업무"
}
```

| 필드 | 타입 | 필수 | 검증 |
|------|------|------|------|
| `name` | String | Y | 1~50자 |

**응답 201**
```json
{
  "success": true,
  "data": {
    "groupId": 1,
    "name": "업무",
    "createdAt": "2024-01-10T08:00:00Z"
  }
}
```

---

### PATCH /api/v1/user-groups/{groupId}
그룹 이름 수정. 본인 그룹만 가능.

**요청**
```json
{
  "name": "업무팀"
}
```

| 필드 | 타입 | 필수 | 검증 |
|------|------|------|------|
| `name` | String | Y | 1~50자 |

**응답 204**

**에러**
| 코드 | 상황 |
|------|------|
| `USER_GROUP_NOT_FOUND` | 존재하지 않는 그룹 |
| `USER_GROUP_ACCESS_DENIED` | 본인 그룹이 아닌 경우 |

---

### DELETE /api/v1/user-groups/{groupId}
그룹 삭제. 그룹에 속한 채팅방 할당은 모두 해제. 본인 그룹만 가능.

**응답 204**

**에러**
| 코드 | 상황 |
|------|------|
| `USER_GROUP_NOT_FOUND` | 존재하지 않는 그룹 |
| `USER_GROUP_ACCESS_DENIED` | 본인 그룹이 아닌 경우 |

---

### GET /api/v1/user-groups/{groupId}/rooms
그룹에 속한 채팅방 목록 조회.

**응답 200**
```json
{
  "success": true,
  "data": [
    {
      "roomId": "550e8400-e29b-41d4-a716-446655440000",
      "type": "GROUP",
      "name": "개발팀",
      "role": "OWNER"
    },
    {
      "roomId": "661f9511-f30c-52e5-b827-557766551111",
      "type": "DM",
      "name": "김철수",
      "role": "MEMBER"
    }
  ]
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `USER_GROUP_NOT_FOUND` | 존재하지 않는 그룹 |
| `USER_GROUP_ACCESS_DENIED` | 본인 그룹이 아닌 경우 |

---

### PUT /api/v1/user-groups/{groupId}/rooms/{roomId}
채팅방을 그룹에 할당. 내가 참여 중인 채팅방만 가능.

**응답 204**

**에러**
| 코드 | 상황 |
|------|------|
| `USER_GROUP_NOT_FOUND` | 존재하지 않는 그룹 |
| `USER_GROUP_ACCESS_DENIED` | 본인 그룹이 아닌 경우 |
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `CHAT_ROOM_ACCESS_DENIED` | 내가 참여 중인 채팅방이 아닌 경우 |

---

### DELETE /api/v1/user-groups/{groupId}/rooms/{roomId}
그룹에서 채팅방 할당 해제.

**응답 204**

**에러**
| 코드 | 상황 |
|------|------|
| `USER_GROUP_NOT_FOUND` | 존재하지 않는 그룹 |
| `USER_GROUP_ACCESS_DENIED` | 본인 그룹이 아닌 경우 |
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |

---

## 7. WebSocket (STOMP)

### 연결

| 항목 | 값 |
|------|-----|
| WebSocket 엔드포인트 | `ws://host/ws` |
| 프로토콜 | STOMP over WebSocket |
| 인증 | STOMP CONNECT 헤더에 `Authorization: Bearer {JWT}` |

**연결 흐름**
```
1. WebSocket 연결  →  ws://host/ws
2. STOMP CONNECT  →  header: Authorization: Bearer {JWT}
3. 채팅방 구독    →  SUBSCRIBE /topic/rooms/{roomId}
4. 메시지 발행    →  SEND /app/rooms/{roomId}/messages
5. 연결 해제      →  STOMP DISCONNECT
```

---

### 구독 토픽

| 토픽 | 설명 |
|------|------|
| `/topic/rooms/{roomId}` | 채팅방의 모든 실시간 이벤트 수신 |

---

### 서버 → 클라이언트 이벤트

모든 이벤트는 `type` 필드로 구분합니다.

#### MESSAGE — 새 메시지
```json
{
  "type": "MESSAGE",
  "data": {
    "messageId": 1704067202005,
    "roomId": "550e8400-e29b-41d4-a716-446655440000",
    "senderId": "user-abc",
    "senderNickname": "홍길동",
    "content": "안녕하세요!",
    "messageType": "TEXT",
    "createdAt": "2024-01-15T09:30:00Z"
  }
}
```

#### MESSAGE_DELETED — 메시지 철회
```json
{
  "type": "MESSAGE_DELETED",
  "data": {
    "messageId": 1704067202005,
    "roomId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

#### MEMBER_JOINED — 멤버 입장
```json
{
  "type": "MEMBER_JOINED",
  "data": {
    "roomId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user-abc",
    "nickname": "홍길동",
    "joinedAt": "2024-01-15T09:30:00Z"
  }
}
```

#### MEMBER_LEFT — 멤버 퇴장 (나가기 / 강제 추방)
```json
{
  "type": "MEMBER_LEFT",
  "data": {
    "roomId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user-abc",
    "reason": "LEFT"
  }
}
```

> `reason`: `LEFT` (본인 나가기) / `KICKED` (강제 추방)

#### READ — 읽음 이벤트
```json
{
  "type": "READ",
  "data": {
    "roomId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user-abc",
    "lastReadMessageId": 1704067202005
  }
}
```

---

### 클라이언트 → 서버 발행

#### 메시지 전송
```
SEND /app/rooms/{roomId}/messages
```
```json
{
  "content": "안녕하세요!",
  "messageType": "TEXT"
}
```

#### 읽음 처리
```
SEND /app/rooms/{roomId}/read
```
```json
{
  "lastReadMessageId": 1704067202005
}
```

---

### 수평 확장 브로드캐스트

서버 인스턴스가 여러 대일 때 Redis Pub/Sub으로 이벤트를 전파합니다.

```
클라이언트 A (서버1)  →  메시지 전송
                          ↓
                    Redis PUBLISH room:{roomId}
                          ↓
            서버1 SUBSCRIBE   서버2 SUBSCRIBE
                ↓                   ↓
         서버1 연결 유저에게    서버2 연결 유저에게
            브로드캐스트          브로드캐스트
```
