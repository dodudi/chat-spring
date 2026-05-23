# 채팅 서비스 API 명세

## 목차

- [공통](#공통)
- [프로필](#프로필)
- [채팅방](#채팅방)
- [초대](#초대)
- [초대 URI](#초대-uri)
- [메시지](#메시지)
- [그룹](#그룹)
- [온라인 상태](#온라인-상태)
- [WebSocket](#websocket)

---

## 공통

### Base URL

```
/api/v1
```

### 인증

모든 API는 `Authorization: Bearer {JWT}` 헤더 필요 (별도 표기 없는 한).

### 응답 형식

```json
// 성공
{ "code": "SUCCESS", "message": null, "data": { ... } }

// 실패
{ "code": "R001", "message": "채팅방을 찾을 수 없습니다.", "data": null }
```

### 공통 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| `C001` | 400 | 요청 값 유효성 오류 |
| `C002` | 401 | 인증 실패 |
| `C003` | 403 | 권한 없음 |
| `C004` | 404 | 리소스 없음 |
| `C005` | 500 | 서버 내부 오류 |

### 사용자 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| `U001` | 404 | 대상 사용자 없음 |

---

## 프로필

### 에러 코드 (Profile)

| 코드 | HTTP | 설명 |
|------|------|------|
| `P001` | 404 | 프로필 없음 |
| `P002` | 403 | 본인 프로필이 아님 |
| `P003` | 409 | 채팅방에서 사용 중인 프로필 (삭제 불가) |

---

### 내 프로필 목록 조회

```
GET /profiles
```

**Response** `200 OK`
```json
[
  {
    "id": 1,
    "nickname": "string",
    "createdAt": "2026-05-22T00:00:00Z"
  }
]
```

---

### 프로필 생성

```
POST /profiles
```

**Request Body**
```json
{
  "nickname": "string"
}
```

**Response** `201 Created`
```json
{
  "id": 2,
  "nickname": "string",
  "createdAt": "2026-05-22T00:00:00Z"
}
```

---

### 프로필 닉네임 수정

> 해당 프로필을 사용 중인 모든 채팅방에 즉시 반영된다.

```
PATCH /profiles/{profileId}
```

**Request Body**
```json
{
  "nickname": "string"
}
```

**Response** `200 OK`

| 에러 코드 | 설명 |
|-----------|------|
| `P001` | 프로필 없음 |
| `P002` | 본인 프로필이 아님 |

---

### 프로필 삭제

> 채팅방에서 사용 중인 프로필은 삭제할 수 없다.

```
DELETE /profiles/{profileId}
```

**Response** `204 No Content`

| 에러 코드 | 설명 |
|-----------|------|
| `P001` | 프로필 없음 |
| `P002` | 본인 프로필이 아님 |
| `P003` | 채팅방에서 사용 중인 프로필 |

---

## 채팅방

### 에러 코드 (Room)

| 코드 | HTTP | 설명 |
|------|------|------|
| `R001` | 404 | 채팅방 없음 |
| `R003` | 409 | 이미 참여 중 |
| `R004` | 409 | 인원 초과 |
| `R005` | 400 | 비밀번호 불일치 |
| `R006` | 403 | 강퇴된 사용자 (초대로만 재참여 가능) |
| `R007` | 403 | 방장 권한 없음 |
| `R008` | 404 | 대상 멤버 없음 |
| `R009` | 404 | 빈 방 — 참여 불가 |
| `R010` | 403 | 지원하지 않는 채팅방 타입 (예: DM 방 초대 시도) |

---

### DM 채팅방 생성

> 동일 두 사용자 간 DM이 이미 존재하면 기존 방을 반환 (find-or-create). 항상 201 반환.
> 기존 방 반환 시에도 요청한 `profileId`로 해당 사용자의 `chat_room_members.profile_id`를 갱신한다.

```
POST /rooms/dm
```

**Request Body**
```json
{
  "targetUserId": "string",
  "profileId": 1
}
```

**Response** `201 Created`
```json
{
  "id": "uuid",
  "type": "DM",
  "roomKey": "dm:hash",
  "createdAt": "2026-05-22T00:00:00Z"
}
```

| 에러 코드 | 설명 |
|-----------|------|
| `U001` | 대상 사용자 없음 |
| `P001` | 프로필 없음 |
| `P002` | 본인 프로필이 아님 |

---

### GROUP 채팅방 생성

```
POST /rooms/group
```

**Request Body**
```json
{
  "name": "string",
  "profileId": 1
}
```

**Response** `201 Created`
```json
{
  "id": "uuid",
  "type": "GROUP",
  "roomKey": "group:uuid",
  "name": "string",
  "createdAt": "2026-05-22T00:00:00Z"
}
```

| 에러 코드 | 설명 |
|-----------|------|
| `P001` | 프로필 없음 |
| `P002` | 본인 프로필이 아님 |

---

### PUBLIC 채팅방 생성

```
POST /rooms/public
```

**Request Body**
```json
{
  "name": "string",
  "password": "string | null",
  "profileId": 1
}
```

> `password: null` 또는 미입력 시 비밀번호 없음.

**Response** `201 Created`
```json
{
  "id": "uuid",
  "type": "PUBLIC",
  "roomKey": "public:uuid",
  "name": "string",
  "hasPassword": false,
  "createdAt": "2026-05-22T00:00:00Z"
}
```

| 에러 코드 | 설명 |
|-----------|------|
| `P001` | 프로필 없음 |
| `P002` | 본인 프로필이 아님 |

---

### 내 채팅방 목록 조회

> 그룹 필터 미지정 시 전체 그룹 기준 반환. `updated_at` 내림차순 정렬, 페이지네이션 없음.
> DM 방의 `name`은 상대방 닉네임으로 동적 채워서 반환한다 (DB는 null).

```
GET /rooms?groupId={groupId}
```

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `groupId` | N | 그룹 필터. 미지정 시 전체 |

**Response** `200 OK`
```json
[
  {
    "id": "uuid",
    "type": "DM | GROUP | PUBLIC",
    "name": "string",
    "lastMessage": "string | null",
    "lastMessageAt": "2026-05-22T00:00:00Z | null",
    "unreadCount": 0,
    "updatedAt": "2026-05-22T00:00:00Z"
  }
]
```

---

### 채팅방 상세 조회

> DM 방의 `name`은 상대방 닉네임으로 동적 채워서 반환한다.

```
GET /rooms/{roomId}
```

**Response** `200 OK`
```json
{
  "id": "uuid",
  "type": "DM | GROUP | PUBLIC",
  "name": "string",
  "hasPassword": false,
  "memberCount": 3,
  "createdAt": "2026-05-22T00:00:00Z"
}
```

| 에러 코드 | 설명 |
|-----------|------|
| `R001` | 채팅방 없음 |

---

### PUBLIC 채팅방 검색

> 빈 방(참여자 0명)은 노출 제외. 인증 필요.

```
GET /rooms/public?name={name}&page={page}&size={size}
```

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `name` | N | 제목 부분 일치 검색 |
| `page` | N | 페이지 번호 (기본값: 0) |
| `size` | N | 페이지 크기 (기본값: 20) |

**Response** `200 OK`
```json
{
  "content": [
    {
      "id": "uuid",
      "name": "string",
      "memberCount": 3,
      "hasPassword": false
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 100
}
```

---

### PUBLIC 채팅방 참여

> 직접 참여 (초대·초대 URI 경유 아님). GROUP 타입에는 호출 불가.

```
POST /rooms/{roomId}/join
```

**Request Body**
```json
{
  "password": "string | null",
  "profileId": 1
}
```

**Response** `200 OK`

| 에러 코드 | 설명 |
|-----------|------|
| `R001` | 채팅방 없음 |
| `R003` | 이미 참여 중 |
| `R004` | 인원 초과 |
| `R005` | 비밀번호 불일치 |
| `R006` | 강퇴된 사용자 |
| `R009` | 빈 방 — 참여 불가 |
| `P001` | 프로필 없음 |
| `P002` | 본인 프로필이 아님 |

---

### 채팅방 정보 수정 (방장 전용)

> GROUP은 `name`만, PUBLIC은 `name`과 `password` 수정 가능.

```
PATCH /rooms/{roomId}
```

**Request Body — GROUP**
```json
{
  "name": "string"
}
```

**Request Body — PUBLIC**
```json
{
  "name": "string",
  "password": "string | null"
}
```

> `password: null` 전달 시 비밀번호 해제.

**Response** `200 OK`

| 에러 코드 | 설명 |
|-----------|------|
| `R001` | 채팅방 없음 |
| `R007` | 방장 권한 없음 |

---

### 채팅방 나가기

```
DELETE /rooms/{roomId}/members/me
```

> - DM: 숨김 처리 (`is_hidden = true`, `hidden_at` 갱신)
> - GROUP: `left_at` 갱신 (초대로 재참여 가능)
> - PUBLIC: 레코드 삭제 (자유 재참여 가능)
> - 방장 나가기 시 `chat_room_members.created_at` 오름차순 기준 다음 멤버에게 자동 위임. 마지막 참여자면 빈 방 상태.
> - 나가기 시 해당 사용자의 `room_group_memberships` 연결을 모두 자동 삭제한다.

**Response** `204 No Content`

| 에러 코드 | 설명 |
|-----------|------|
| `R001` | 채팅방 없음 |

---

### 방장 위임 (방장 전용)

> 나가지 않고 다른 멤버에게 방장 권한 이전.

```
PUT /rooms/{roomId}/owner
```

**Request Body**
```json
{
  "newOwnerId": "string"
}
```

**Response** `200 OK`

| 에러 코드 | 설명 |
|-----------|------|
| `R001` | 채팅방 없음 |
| `R007` | 방장 권한 없음 |
| `R008` | 대상 멤버 없음 |

---

### 멤버 강퇴 (방장 전용)

```
DELETE /rooms/{roomId}/members/{userId}
```

**Response** `204 No Content`

| 에러 코드 | 설명 |
|-----------|------|
| `R001` | 채팅방 없음 |
| `R007` | 방장 권한 없음 |
| `R008` | 대상 멤버 없음 |

---

### 채팅방 멤버 목록 조회

```
GET /rooms/{roomId}/members
```

**Response** `200 OK`
```json
[
  {
    "userId": "string",
    "nickname": "string",
    "role": "OWNER | MEMBER",
    "online": true
  }
]
```

| 에러 코드 | 설명 |
|-----------|------|
| `R001` | 채팅방 없음 |

---

### 채팅방 사용 프로필 변경

> 현재 방에서 사용 중인 프로필을 다른 프로필로 교체한다.

```
PUT /rooms/{roomId}/members/me/profile
```

**Request Body**
```json
{
  "profileId": 2
}
```

**Response** `200 OK`

| 에러 코드 | 설명 |
|-----------|------|
| `R001` | 채팅방 없음 |
| `P001` | 프로필 없음 |
| `P002` | 본인 프로필이 아님 |

---

> **구현 메모 — GROUP 재참여**
> 초대 수락으로 GROUP에 재참여 시 `chat_room_members`의 기존 레코드를 재사용한다.
> `(room_id, user_id)` UNIQUE 제약으로 새 레코드 삽입 불가.
> `left_at = NULL`, `kicked_at = NULL`로 리셋, `profile_id`를 요청의 `profileId`로 갱신.
> (`kicked_at` 초기화: 방장이 재초대한 이상 강퇴 이력 해소로 처리)

---

## 초대

### 에러 코드 (Invitation)

| 코드 | HTTP | 설명 |
|------|------|------|
| `I001` | 404 | 초대 없음 |
| `I002` | 409 | 이미 처리된 초대 (ACCEPTED 또는 REJECTED) |
| `I003` | 409 | 이미 참여 중인 사용자 |
| `I004` | 409 | PENDING 상태의 초대 이미 존재 |

---

### 초대 발송 (방장 전용)

> DM 채팅방은 초대 불가. 강퇴된 사용자도 초대로 재참여 가능.

```
POST /rooms/{roomId}/invitations
```

**Request Body**
```json
{
  "inviteeId": "string"
}
```

**Response** `201 Created`
```json
{
  "id": 1,
  "roomId": "uuid",
  "inviteeId": "string",
  "status": "PENDING",
  "createdAt": "2026-05-22T00:00:00Z"
}
```

| 에러 코드 | 설명 |
|-----------|------|
| `R001` | 채팅방 없음 |
| `R007` | 방장 권한 없음 |
| `R010` | DM 채팅방 — 초대 불가 |
| `U001` | 대상 사용자 없음 |
| `I003` | 이미 참여 중인 사용자 |
| `I004` | PENDING 초대 이미 존재 |

---

### 내 초대 목록 조회

> 내가 받은 초대 목록. `status` 미지정 시 전체 반환.

```
GET /invitations?status={status}
```

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `status` | N | `PENDING \| ACCEPTED \| REJECTED`. 미지정 시 전체 |

**Response** `200 OK`
```json
[
  {
    "id": 1,
    "roomId": "uuid",
    "roomName": "string | null",
    "inviterId": "string",
    "status": "PENDING",
    "createdAt": "2026-05-22T00:00:00Z"
  }
]
```

---

### 초대 수락

> 상태 전이 명령이므로 POST 사용 (PATCH는 부분 수정 의미라 부적합).

```
POST /invitations/{invitationId}/accept
```

**Request Body**
```json
{
  "profileId": 1
}
```

> GROUP 재참여 시 기존 레코드를 재사용: `left_at = NULL`, `kicked_at = NULL` 리셋, `profile_id`를 선택한 프로필로 갱신.
> 동시에 해당 채팅방을 사용자의 기본 그룹(`is_default = true`)에 자동으로 재연결(`room_group_memberships` 레코드 삽입).
> 기본 그룹이 없으면 이 단계는 건너뛴다.

**Response** `200 OK`

| 에러 코드 | 설명 |
|-----------|------|
| `I001` | 초대 없음 |
| `I002` | 이미 처리된 초대 |
| `P001` | 프로필 없음 |
| `P002` | 본인 프로필이 아님 |
| `R004` | 인원 초과 (초대~수락 사이 방이 꽉 찬 경우) |
| `R009` | 빈 방 (초대~수락 사이 방이 빈 방이 된 경우) |

---

### 초대 거절

> 상태 전이 명령이므로 POST 사용.

```
POST /invitations/{invitationId}/reject
```

**Response** `200 OK`

| 에러 코드 | 설명 |
|-----------|------|
| `I001` | 초대 없음 |
| `I002` | 이미 처리된 초대 |

---

## 초대 URI

### 에러 코드 (InviteLink)

| 코드 | HTTP | 설명 |
|------|------|------|
| `L001` | 410 | 만료된 초대 링크 |
| `L002` | 410 | 비활성화된 초대 링크 |

---

### 초대 URI 생성 (방장 전용)

> GROUP/PUBLIC 채팅방만 가능. 생성 개수 제한 없음. `expiresAt: null` 시 만료 없음.

```
POST /rooms/{roomId}/invite-links
```

**Request Body**
```json
{
  "expiresAt": "2026-06-01T00:00:00Z | null"
}
```

**Response** `201 Created`
```json
{
  "id": 1,
  "token": "string",
  "inviteUrl": "/api/v1/invite-links/{token}/join",
  "expiresAt": "2026-06-01T00:00:00Z | null",
  "isActive": true,
  "createdAt": "2026-05-22T00:00:00Z"
}
```

| 에러 코드 | 설명 |
|-----------|------|
| `R001` | 채팅방 없음 |
| `R007` | 방장 권한 없음 |

---

### 초대 URI 목록 조회 (방장 전용)

> 활성화된 링크(`is_active = true`)만 반환.

```
GET /rooms/{roomId}/invite-links
```

**Response** `200 OK`
```json
[
  {
    "id": 1,
    "token": "string",
    "inviteUrl": "/api/v1/invite-links/{token}/join",
    "expiresAt": "2026-06-01T00:00:00Z | null",
    "createdAt": "2026-05-22T00:00:00Z"
  }
]
```

| 에러 코드 | 설명 |
|-----------|------|
| `R001` | 채팅방 없음 |
| `R007` | 방장 권한 없음 |

---

### 초대 URI로 채팅방 입장

> 비밀번호 입력 생략 가능. 이미 참여 중이면 에러 없이 200 반환.
> 강퇴(`kicked_at`) 또는 자발 퇴장(`left_at`) 사용자도 초대 URI를 통해 재참여 가능하다 — `R006` 미적용.
> GROUP 재참여 시 기존 레코드를 재사용: `left_at = NULL`, `kicked_at = NULL` 리셋, `profile_id`를 선택한 프로필로 갱신.
> 동시에 해당 채팅방을 사용자의 기본 그룹(`is_default = true`)에 자동으로 재연결(`room_group_memberships` 레코드 삽입).
> 기본 그룹이 없으면 이 단계는 건너뛴다.

```
POST /invite-links/{token}/join
```

**Request Body**
```json
{
  "profileId": 1
}
```

**Response** `200 OK`
```json
{
  "roomId": "uuid",
  "type": "GROUP | PUBLIC"
}
```

| 에러 코드 | 설명 |
|-----------|------|
| `L001` | 만료된 초대 링크 |
| `L002` | 비활성화된 초대 링크 |
| `R004` | 인원 초과 |
| `R009` | 빈 방 — 참여 불가 |
| `P001` | 프로필 없음 |
| `P002` | 본인 프로필이 아님 |

---

### 초대 URI 비활성화 (방장 전용)

```
DELETE /rooms/{roomId}/invite-links/{linkId}
```

**Response** `204 No Content`

| 에러 코드 | 설명 |
|-----------|------|
| `R001` | 채팅방 없음 |
| `R007` | 방장 권한 없음 |

---

## 메시지

### 에러 코드 (Message)

| 코드 | HTTP | 설명 |
|------|------|------|
| `M001` | 404 | 메시지 없음 |
| `M002` | 403 | 본인 메시지가 아님 |

---

### 메시지 히스토리 조회

> - 커서 기반 페이징. `before` 미지정 시 최신 메시지부터.
> - DM: `hidden_at` 이전 메시지는 서버가 자동 제외.
> - 철회된 메시지(`deleted_at IS NOT NULL`)는 응답에서 완전 제외.

```
GET /rooms/{roomId}/messages?before={messageId}&size={size}
```

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `before` | N | 이 메시지 ID 이전 항목 조회. 미지정 시 최신부터 |
| `size` | N | 페이지 크기 (기본값: 50) |

**Response** `200 OK`
```json
{
  "messages": [
    {
      "id": 1,
      "senderId": "string",
      "senderNickname": "string",
      "content": "string",
      "type": "TEXT",
      "isEdited": false,
      "createdAt": "2026-05-22T00:00:00Z"
    }
  ],
  "hasNext": true,
  "nextCursor": 1
}
```

| 에러 코드 | 설명 |
|-----------|------|
| `R001` | 채팅방 없음 |

---

### 메시지 수정

> 본인 메시지만 수정 가능. 수정 시간 제한 없음. 수정 시 `is_edited = true` 로 마킹.

```
PATCH /messages/{messageId}
```

**Request Body**
```json
{
  "content": "string"   // 최대 1,000자
}
```

**Response** `200 OK`

| 에러 코드 | 설명 |
|-----------|------|
| `M001` | 메시지 없음 |
| `M002` | 본인 메시지가 아님 |
| `C001` | 유효성 오류 (1,000자 초과 등) |

---

### 메시지 철회

> 소프트 딜리트 (`deleted_at` 마킹). 조회 응답에서 완전 제외.
> 철회 이벤트는 WebSocket으로 실시간 전달 → 클라이언트가 UI에서 제거.

```
DELETE /messages/{messageId}
```

**Response** `204 No Content`

| 에러 코드 | 설명 |
|-----------|------|
| `M001` | 메시지 없음 |
| `M002` | 본인 메시지가 아님 |

---

### 읽음 처리

> 같은 값으로 여러 번 호출해도 결과가 동일한 멱등 연산이므로 PUT 사용.
> WebSocket 연결 중에는 STOMP SEND로도 처리 가능. REST는 백그라운드·재연결 시 사용.

```
PUT /rooms/{roomId}/messages/read
```

**Request Body**
```json
{
  "lastReadMessageId": 42
}
```

**Response** `200 OK`

| 에러 코드 | 설명 |
|-----------|------|
| `R001` | 채팅방 없음 |

---

## 그룹

### 에러 코드 (Group)

| 코드 | HTTP | 설명 |
|------|------|------|
| `G001` | 404 | 그룹 없음 |
| `G002` | 409 | 그룹 이름 중복 |
| `G003` | 409 | 최대 그룹 수 초과 (기본값: 10개) |
| `G004` | 403 | 기본 그룹 변경 불가 (수정·삭제·채팅방 제거 포함) |

---

### 그룹 목록 조회

```
GET /groups
```

**Response** `200 OK`
```json
[
  {
    "id": 1,
    "name": "전체",
    "isDefault": true
  },
  {
    "id": 2,
    "name": "업무",
    "isDefault": false
  }
]
```

---

### 그룹 생성

> 최대 10개. 이름 중복 불가 (사용자 기준). 기본 그룹(`isDefault: false`).

```
POST /groups
```

**Request Body**
```json
{
  "name": "string"
}
```

**Response** `201 Created`
```json
{
  "id": 2,
  "name": "string",
  "isDefault": false
}
```

| 에러 코드 | 설명 |
|-----------|------|
| `G002` | 그룹 이름 중복 |
| `G003` | 최대 그룹 수 초과 |

---

### 그룹 수정

```
PATCH /groups/{groupId}
```

**Request Body**
```json
{
  "name": "string"
}
```

**Response** `200 OK`

| 에러 코드 | 설명 |
|-----------|------|
| `G001` | 그룹 없음 |
| `G002` | 그룹 이름 중복 |
| `G004` | 기본 그룹 변경 불가 |

---

### 그룹 삭제

> 포함된 채팅방은 그룹 연결만 제거, 참여는 유지.

```
DELETE /groups/{groupId}
```

**Response** `204 No Content`

| 에러 코드 | 설명 |
|-----------|------|
| `G001` | 그룹 없음 |
| `G004` | 기본 그룹 변경 불가 |

---

### 채팅방 그룹 할당

> 이미 해당 그룹에 속해 있어도 에러 없이 200 반환 (idempotent).

```
PUT /groups/{groupId}/rooms/{roomId}
```

**Response** `200 OK`

| 에러 코드 | 설명 |
|-----------|------|
| `G001` | 그룹 없음 |
| `R001` | 채팅방 없음 |

---

### 채팅방 그룹 제거

> 기본 그룹(`is_default = true`)에서는 제거 불가.

```
DELETE /groups/{groupId}/rooms/{roomId}
```

**Response** `204 No Content`

| 에러 코드 | 설명 |
|-----------|------|
| `G001` | 그룹 없음 |
| `G004` | 기본 그룹 변경 불가 |
| `R001` | 채팅방 없음 |

---

## 온라인 상태

### 온라인 상태 조회

```
GET /users/{userId}/presence
```

**Response** `200 OK`
```json
{
  "userId": "string",
  "online": true
}
```

---

## WebSocket

### 연결

```
STOMP CONNECT
ws://{host}/ws/chat
Authorization: Bearer {JWT}
```

> 인증 실패 시 STOMP ERROR frame 반환 후 연결 종료.
> CONNECT 성공 직후 서버가 PENDING 상태의 초대 목록을 `/user/queue/notifications`로 자동 푸시.

---

### 메시지 전송

> DM 방에서 메시지를 전송하면 서버가 `is_hidden = true`인 상대 멤버의 숨김을 자동 해제한다 (`is_hidden = false`).
> 숨김 해제된 멤버는 이후 방 목록 조회 시 해당 DM 방이 다시 노출된다.

```
SEND /app/rooms/{roomId}/messages

{
  "content": "string",   // 최대 1,000자
  "type": "TEXT"
}
```

**브로드캐스트** → `/topic/rooms/{roomId}`
```json
{
  "type": "MESSAGE",
  "id": 1,
  "senderId": "string",
  "senderNickname": "string",
  "content": "string",
  "messageType": "TEXT",
  "isEdited": false,
  "createdAt": "2026-05-22T00:00:00Z"
}
```

---

### 방장 변경 이벤트 수신

> 나가기 자동 위임(`DELETE /rooms/{roomId}/members/me`) 또는 수동 위임(`PUT /rooms/{roomId}/owner`) 시 브로드캐스트.

**브로드캐스트** → `/topic/rooms/{roomId}`
```json
{
  "type": "OWNER_CHANGED",
  "newOwnerId": "string"
}
```

---

### 프로필 닉네임 변경 이벤트 수신

> REST API(`PATCH /profiles/{id}`) 호출 시 해당 프로필을 사용 중인 모든 채팅방 참여자에게 브로드캐스트.

**브로드캐스트** → `/topic/rooms/{roomId}` (프로필 사용 중인 각 방)
```json
{
  "type": "PROFILE_UPDATED",
  "userId": "string",
  "nickname": "string"
}
```

---

### 메시지 수정 이벤트 수신

> REST API(`PATCH /messages/{id}`) 호출 시 서버가 해당 방 참여자 전원에게 브로드캐스트.

**브로드캐스트** → `/topic/rooms/{roomId}`
```json
{
  "type": "MESSAGE_EDITED",
  "messageId": 42,
  "content": "string",
  "isEdited": true
}
```

---

### 메시지 철회 이벤트 수신

> REST API(`DELETE /messages/{id}`) 호출 시 서버가 해당 방 참여자 전원에게 브로드캐스트.

**브로드캐스트** → `/topic/rooms/{roomId}`
```json
{
  "type": "MESSAGE_DELETED",
  "messageId": 42
}
```

---

### 읽음 처리

```
SEND /app/rooms/{roomId}/read

{
  "lastReadMessageId": 42
}
```

---

### heartbeat

> 권장 주기: **30초**. TTL은 60초이며, 30초 주기로 갱신하면 네트워크 지연에 안전한 마진 확보.

```
SEND /app/presence/heartbeat
```

---

### 개인 알림 수신

```
SUBSCRIBE /user/queue/notifications
```

**초대 알림 payload** (실시간 수신 및 CONNECT 직후 PENDING 초대 자동 전달)
```json
{
  "type": "INVITATION",
  "invitationId": 1,
  "roomId": "uuid",
  "roomName": "string | null",
  "inviterId": "string"
}
```

---

### 에러 수신

```
SUBSCRIBE /user/queue/errors
```

```json
{
  "code": "R001",
  "message": "채팅방을 찾을 수 없습니다."
}
```
