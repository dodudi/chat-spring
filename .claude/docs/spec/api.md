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

> DM 타입의 `name`은 상대방 `profiles.nickname` 값. 요청한 사용자의 userId와 `dm_rooms.user1_id` / `user2_id`를 비교해 상대방 userId를 판별한 후 조회.

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

> DM 타입의 `name`은 상대방 `profiles.nickname` 값. 목록 조회와 동일한 방식으로 판별.

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `CHAT_ROOM_ACCESS_DENIED` | 멤버가 아닌 경우 |

---

### DELETE /api/v1/chat-rooms/{roomId}
채팅방 삭제. OWNER 전용.

> 소프트 삭제: `chat_rooms.deleted_at` 기록. 메시지·멤버·초대 데이터는 유지.
> 삭제된 채팅방은 모든 조회에서 제외. Redis `messages:{roomId}`, `read_cursor:{roomId}` 키 삭제.
> `room_group_memberships`에서 해당 roomId 레코드 모두 삭제.
> 현재 구독 중인 멤버에게 `ROOM_DELETED` WebSocket 이벤트 브로드캐스트.

**응답 204**

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `MEMBER_PERMISSION_DENIED` | OWNER가 아닌 경우 |

---

### POST /api/v1/chat-rooms/dm
DM 채팅방 생성. 이미 존재하면 기존 방 반환. 소프트 삭제된 방이 있으면 재활성화.

**요청**
```json
{
  "targetUserId": "user-xyz"
}
```

| 필드 | 타입 | 필수 | 검증 |
|------|------|------|------|
| `targetUserId` | String | Y | 본인 ID 불가 |

**응답 200** (기존 방 반환 또는 재활성화) / **201** (신규 생성)
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
| `CANNOT_DM_SELF` | 본인에게 DM 시도 |

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
| `cursor` | String (UUID) | — | 마지막 조회 roomId. 없으면 처음부터 |
| `size` | Int | 20 | 조회 개수 |
| `keyword` | String | — | 채널명 검색 |

> 정렬 기준: `created_at DESC`. cursor 수신 시 서버는 해당 roomId의 `created_at`을 조회한 뒤 `WHERE created_at < :pivot ORDER BY created_at DESC, room_id ASC LIMIT :size` 실행.

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
        "memberCount": 42,
        "createdAt": "2024-01-15T09:30:00Z"
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

> 이전에 나갔던 사용자가 재참여하면 기존 `chat_room_members` row를 재활성화 (`left_at = NULL`, `joined_at` 갱신, `role = MEMBER`, `is_hidden = false`, `hidden_at = NULL`).

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
| `MEMBER_KICKED_OUT` | 강퇴된 사용자 |

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
멤버 역할 변경. OWNER 전용. 본인(OWNER) 역할은 변경 불가.

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
| `CANNOT_CHANGE_OWNER_ROLE` | 본인(OWNER) 역할 변경 시도 |

---

### DELETE /api/v1/chat-rooms/{roomId}/members/me
채팅방 나가기. `left_at` 기록. `room_group_memberships`에서 해당 방 할당 자동 해제.

> OWNER는 나가기 불가. 방을 없애려면 채팅방 삭제(`DELETE /api/v1/chat-rooms/{roomId}`)를 사용.

**응답 204**

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `MEMBER_NOT_FOUND` | 이미 나간 상태 |
| `OWNER_CANNOT_LEAVE` | OWNER는 나가기 불가 |

---

### PATCH /api/v1/chat-rooms/{roomId}/members/me/hidden
채팅방 나만 숨기기. 상대방에게는 유지, 내 목록에서만 제거. `is_hidden = true` 기록.

> 이미 숨김 상태여도 204 반환 (멱등).

**응답 204**

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `MEMBER_NOT_FOUND` | 멤버가 아닌 경우 |

---

### DELETE /api/v1/chat-rooms/{roomId}/members/{userId}
멤버 강제 추방. OWNER / ADMIN 전용. `chat_room_members`에서 해당 row 제거 후 `room_bans`에 이력 기록. `room_group_memberships`에서 해당 사용자의 이 채팅방 할당 자동 해제.

> 추방 권한: OWNER는 ADMIN·MEMBER 추방 가능. ADMIN은 MEMBER만 추방 가능 (같은 레벨 ADMIN 추방 불가). OWNER는 추방 불가.

**응답 204**

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `MEMBER_NOT_FOUND` | 대상 멤버 없음 |
| `MEMBER_PERMISSION_DENIED` | OWNER/ADMIN이 아닌 경우, 또는 ADMIN이 ADMIN 추방 시도 |
| `CANNOT_KICK_OWNER` | OWNER 추방 시도 |

---

## 4. 초대

### POST /api/v1/chat-rooms/{roomId}/invitations
특정 사용자에게 초대장 발송. GROUP 타입 채팅방만 가능. 멤버 누구나 가능.

> `expires_at`은 서버가 생성 시각 기준 **24시간** 후로 자동 설정.

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
    "expiresAt": "2024-01-16T09:30:00Z",
    "createdAt": "2024-01-15T09:30:00Z"
  }
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `CHAT_ROOM_ACCESS_DENIED` | 멤버가 아닌 경우 |
| `INVALID_ROOM_TYPE` | DM·PUBLIC 방에 초대 시도 |
| `PROFILE_NOT_FOUND` | 존재하지 않는 inviteeId |
| `MEMBER_ALREADY_EXISTS` | 대상이 이미 멤버 |
| `INVITATION_ALREADY_PROCESSED` | 이미 PENDING 초대 존재 |

---

### GET /api/v1/invitations
내게 온 PENDING 초대 목록 조회. 만료된 초대(`expires_at < 현재 시각`)는 제외.

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

> 만료된 초대(`expires_at < 현재 시각`)는 수락/거절 불가. 별도 배치 없이 접근 시점에 만료 여부 검사(Lazy evaluation).
> `ACCEPTED` 시 `chat_room_members`에 추가. 이전에 나갔던 사용자(`left_at IS NOT NULL`)는 재활성화 (`left_at = NULL`, `joined_at` 갱신, `role = MEMBER`, `is_hidden = false`, `hidden_at = NULL`). WebSocket `MEMBER_JOINED` 이벤트 발행.
> 초대 대상이 `room_bans`에 등록된 사용자인 경우 (OWNER/ADMIN이 직접 초대로 복귀 허용): `room_bans` 레코드 삭제 후 `chat_room_members` 추가.

**에러**
| 코드 | 상황 |
|------|------|
| `INVITATION_NOT_FOUND` | 존재하지 않는 초대 또는 본인 초대가 아닌 경우 |
| `INVITATION_EXPIRED` | 만료된 초대 |
| `INVITATION_ALREADY_PROCESSED` | 이미 수락/거절된 초대 |

---

### POST /api/v1/chat-rooms/{roomId}/invite-links
초대 링크 생성. GROUP 타입 채팅방만 가능. 멤버 누구나 가능.

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
| `INVALID_ROOM_TYPE` | DM·PUBLIC 방에 초대 링크 생성 시도 |

---

### GET /api/v1/chat-rooms/{roomId}/invite-links
채팅방의 초대 링크 목록 조회 (만료·비활성 포함 전체). 멤버 누구나 가능.

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
초대 링크 비활성화. `is_active = false` 처리. 링크 생성자 또는 OWNER/ADMIN만 가능.

**응답 204**

**에러**
| 코드 | 상황 |
|------|------|
| `INVITE_LINK_NOT_FOUND` | 존재하지 않는 링크 |
| `CHAT_ROOM_ACCESS_DENIED` | 멤버가 아닌 경우 |
| `MEMBER_PERMISSION_DENIED` | 생성자/OWNER/ADMIN이 아닌 경우 |

---

### POST /api/v1/invite-links/{token}/join
초대 링크 토큰으로 채팅방 참여.

> 이전에 나갔던 사용자가 재참여하면 기존 `chat_room_members` row를 재활성화 (`left_at = NULL`, `joined_at` 갱신, `role = MEMBER`, `is_hidden = false`, `hidden_at = NULL`).

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
| `MEMBER_KICKED_OUT` | 강퇴된 사용자 |

---

## 5. 메시지

### POST /api/v1/chat-rooms/{roomId}/messages
메시지 전송. WebSocket 연결이 없을 때 HTTP fallback으로도 사용 가능.

> HTTP로 전송한 메시지도 동일하게 Redis Pub/Sub을 통해 WebSocket 구독자에게 `MESSAGE` 이벤트로 브로드캐스트.

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
    "createdAt": "2024-01-15T09:30:00Z",
    "unreadCount": 4
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
        "deleted": false,
        "unreadCount": 2
      },
      {
        "messageId": 1704067201001,
        "senderId": "user-xyz",
        "senderNickname": "김철수",
        "content": null,
        "messageType": "TEXT",
        "createdAt": "2024-01-15T09:30:01Z",
        "deleted": true,
        "unreadCount": 0
      }
    ],
    "nextCursor": 1704067201001,
    "hasNext": true
  }
}
```

> 철회된 메시지는 `deleted: true`, `content: null` 로 반환.
> `unreadCount`: 해당 메시지를 아직 읽지 않은 활성 멤버 수. Redis `read_cursor:{roomId}`에서 cursor가 해당 messageId보다 작은 멤버 수로 계산. Redis 장애 시 0 반환.

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

> `lastReadMessageId`가 해당 채팅방(`roomId`)에 속한 메시지인지 검증. 다른 방의 messageId를 넘기면 `MESSAGE_NOT_FOUND` 반환.
> 처리 완료 후 `/topic/rooms/{roomId}`로 `READ` 이벤트 브로드캐스트.

**응답 204**

**에러**
| 코드 | 상황 |
|------|------|
| `CHAT_ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| `CHAT_ROOM_ACCESS_DENIED` | 멤버가 아닌 경우 |
| `MESSAGE_NOT_FOUND` | 해당 채팅방에 존재하지 않는 messageId |

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

> 이미 해당 그룹에 할당된 채팅방이면 204 반환 (멱등).

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

> 구독 시 서버에서 채팅방이 활성 상태(`deleted_at IS NULL`)이고 활성 멤버(`left_at IS NULL`)인지 검증. 조건 미충족 시 구독 거부 (STOMP ERROR 프레임 반환).

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
    "createdAt": "2024-01-15T09:30:00Z",
    "unreadCount": 4
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

> 발행 조건: 초대 수락(`ACCEPTED`), 초대 링크 참여, PUBLIC 채널 참여 세 경우 모두 발행.

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

#### ROOM_DELETED — 채팅방 삭제
```json
{
  "type": "ROOM_DELETED",
  "data": {
    "roomId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

---

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

> `lastReadMessageId`가 해당 `roomId`에 속한 메시지인지 검증. 유효하지 않으면 무시.

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
