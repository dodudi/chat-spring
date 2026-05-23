# 채팅 서비스 인터페이스 설계

## 도메인·패키지 구조

| 도메인 | 패키지 | Service 인터페이스 | 구현체 |
|--------|--------|--------------------|--------|
| User | `com.chat.user` | `UserService` | `UserServiceImpl` |
| Profile | `com.chat.profile` | `ProfileService` | `ProfileServiceImpl` |
| Room | `com.chat.room` | `RoomService` | `RoomServiceImpl` |
| Invitation | `com.chat.invitation` | `InvitationService` | `InvitationServiceImpl` |
| InviteLink | `com.chat.invitelink` | `InviteLinkService` | `InviteLinkServiceImpl` |
| Message | `com.chat.message` | `MessageService` | `MessageServiceImpl` |
| Group | `com.chat.group` | `GroupService` | `GroupServiceImpl` |
| Presence | `com.chat.websocket.presence` | `PresenceService` | `PresenceServiceImpl` |

## 공통 규칙

- 인증된 사용자 식별자는 `String userId` (JWT `sub` 값) — 메서드 첫 번째 파라미터
- 채팅방 ID 타입: `UUID` (DB `uuid` 타입 기준)
- 비즈니스 예외: `AppException(ErrorCode)` 로 던진다
- DTO: `record` 사용
- **WebSocket 이벤트 발행 위치**
  - REST API 트리거 이벤트(`MESSAGE_EDITED`, `MESSAGE_DELETED`, `PROFILE_UPDATED`, `OWNER_CHANGED`, `INVITATION`): Service 내부에서 Redis pub 처리
  - WebSocket 트리거 이벤트(`MESSAGE`): `StompChatHandler`가 `MessageService.sendMessage()` 반환값으로 Redis pub 처리
- **initUser 트리거**: `HandlerInterceptor` — 인증된 모든 API 요청 진입 시 `UserService.initUser()` 자동 호출 (멱등)

---

## 1. UserService

```java
package com.chat.user.application;

public interface UserService {
    /**
     * 최초 API 호출 시 users 테이블 upsert + 기본 프로필 1개 + 기본 그룹 1개 생성.
     * 이미 존재하면 아무 작업도 하지 않는다 (멱등).
     * HandlerInterceptor에서 모든 인증 요청마다 호출된다.
     */
    void initUser(String userId);

    /**
     * DM 생성·초대 발송 시 대상 사용자 존재 여부 검증.
     * 없으면 AppException(U001).
     */
    void validateExists(String targetUserId);
}
```

---

## 2. ProfileService

```java
package com.chat.profile.application;

public interface ProfileService {
    List<ProfileResponse> getMyProfiles(String userId);

    ProfileResponse createProfile(String userId, CreateProfileRequest request);

    /**
     * 예외: P001(없음), P002(본인 아님)
     * 사이드이펙트: 해당 프로필을 사용 중인 모든 채팅방에 PROFILE_UPDATED 이벤트 브로드캐스트
     */
    ProfileResponse updateNickname(String userId, Long profileId, UpdateProfileRequest request);

    /**
     * 예외: P001(없음), P002(본인 아님), P003(채팅방에서 사용 중)
     * 삭제된 프로필을 참조하는 메시지의 senderNickname은 빈 문자열로 표시된다 (ON DELETE SET NULL).
     */
    void deleteProfile(String userId, Long profileId);
}
```

### DTOs

```java
// Request
public record CreateProfileRequest(
        @NotBlank String nickname
) {}

public record UpdateProfileRequest(
        @NotBlank String nickname
) {}

// Response
public record ProfileResponse(
        Long id,
        String nickname,
        OffsetDateTime createdAt
) {
    public static ProfileResponse from(Profile profile) { ... }
}
```

---

## 3. RoomService

```java
package com.chat.room.application;

public interface RoomService {

    // ── 생성 ────────────────────────────────────────────
    /**
     * 동일 두 사용자 간 DM 이미 존재하면 기존 방 반환 (find-or-create). 항상 201.
     * 기존 방 반환 시에도 요청한 profileId로 chat_room_members.profile_id 갱신.
     * 예외: U001(대상 없음), P001(프로필 없음), P002(본인 프로필 아님)
     */
    DmRoomResponse createDmRoom(String userId, CreateDmRoomRequest request);

    /** 예외: P001, P002 */
    RoomResponse createGroupRoom(String userId, CreateGroupRoomRequest request);

    /** 예외: P001, P002 */
    PublicRoomResponse createPublicRoom(String userId, CreatePublicRoomRequest request);

    // ── 조회 ────────────────────────────────────────────
    /** groupId null 이면 전체 그룹 기준 반환. DM 방 name은 상대 닉네임으로 동적 채워서 반환. */
    List<RoomSummaryResponse> getMyRooms(String userId, Long groupId);

    /** 예외: R001 */
    RoomDetailResponse getRoom(UUID roomId);

    /** 빈 방(참여자 0명) 제외. */
    Page<PublicRoomSummaryResponse> searchPublicRooms(String name, Pageable pageable);

    // ── 참여·나가기·수정 ──────────────────────────────────
    /** 예외: R001, R003, R004, R005, R006, R009, P001, P002 */
    void joinPublicRoom(String userId, UUID roomId, JoinRoomRequest request);

    /** 예외: R001, R007. GROUP은 name만, PUBLIC은 name+password 수정 가능. */
    void updateRoom(String userId, UUID roomId, UpdateRoomRequest request);

    /**
     * DM: is_hidden=true + hidden_at 갱신.
     * GROUP: left_at 갱신. 나가기 시 room_group_memberships 자동 삭제.
     * PUBLIC: 레코드 삭제. room_group_memberships 자동 삭제.
     * 사이드이펙트: 방장 나가기 시 created_at 오름차순 다음 멤버에게 자동 위임 → OWNER_CHANGED 브로드캐스트.
     * 예외: R001
     */
    void leaveRoom(String userId, UUID roomId);

    /**
     * 사이드이펙트: OWNER_CHANGED 이벤트 브로드캐스트.
     * 예외: R001, R007, R008
     */
    void delegateOwner(String userId, UUID roomId, DelegateOwnerRequest request);

    /** 예외: R001, R007, R008 */
    void kickMember(String userId, UUID roomId, String targetUserId);

    /** 예외: R001 */
    List<RoomMemberResponse> getMembers(UUID roomId);

    /** 예외: R001, P001, P002 */
    void changeProfile(String userId, UUID roomId, ChangeProfileRequest request);
}
```

### DTOs

```java
// Request
public record CreateDmRoomRequest(
        @NotBlank String targetUserId,
        @NotNull Long profileId
) {}

public record CreateGroupRoomRequest(
        @NotBlank String name,
        @NotNull Long profileId
) {}

public record CreatePublicRoomRequest(
        @NotBlank String name,
        String password,        // null = 비밀번호 없음
        @NotNull Long profileId
) {}

public record JoinRoomRequest(
        String password,        // null = 비밀번호 없음
        @NotNull Long profileId
) {}

public record UpdateRoomRequest(
        String name,
        String password         // null 전달 시 비밀번호 해제
) {}

public record DelegateOwnerRequest(
        @NotBlank String newOwnerId
) {}

public record ChangeProfileRequest(
        @NotNull Long profileId
) {}

// Response
public record DmRoomResponse(
        UUID id, String type, String roomKey, OffsetDateTime createdAt
) {}

public record RoomResponse(
        UUID id, String type, String roomKey, String name, OffsetDateTime createdAt
) {}

public record PublicRoomResponse(
        UUID id, String type, String roomKey, String name, boolean hasPassword, OffsetDateTime createdAt
) {}

public record RoomSummaryResponse(
        UUID id, String type, String name,
        String lastMessage, OffsetDateTime lastMessageAt,
        int unreadCount, OffsetDateTime updatedAt
) {}

public record RoomDetailResponse(
        UUID id, String type, String name, boolean hasPassword, int memberCount, OffsetDateTime createdAt
) {}

public record PublicRoomSummaryResponse(
        UUID id, String name, int memberCount, boolean hasPassword
) {}

public record RoomMemberResponse(
        String userId, String nickname, String role, boolean online
) {}
```

---

## 4. InvitationService

```java
package com.chat.invitation.application;

public interface InvitationService {

    /**
     * 사이드이펙트: INVITATION 이벤트를 invitee 개인 채널(/user/queue/notifications)로 전송.
     * 예외: R001, R007, R010(DM 초대 불가), U001, I003(이미 참여), I004(PENDING 중복)
     */
    InvitationResponse sendInvitation(String userId, UUID roomId, SendInvitationRequest request);

    /** status null이면 전체 반환. */
    List<InvitationResponse> getMyInvitations(String userId, InvitationStatus status);

    /**
     * GROUP 재참여: left_at=NULL, kicked_at=NULL 리셋 + profile_id 갱신 + 기본 그룹 재연결.
     * 예외: I001, I002, P001, P002, R004(인원 초과), R009(빈 방)
     */
    void acceptInvitation(String userId, Long invitationId, AcceptInvitationRequest request);

    /** 예외: I001, I002 */
    void rejectInvitation(String userId, Long invitationId);
}
```

### DTOs

```java
public record SendInvitationRequest(
        @NotBlank String inviteeId
) {}

public record AcceptInvitationRequest(
        @NotNull Long profileId
) {}

public record InvitationResponse(
        Long id, UUID roomId, String roomName,
        String inviterId, InvitationStatus status, OffsetDateTime createdAt
) {}
```

---

## 5. InviteLinkService

```java
package com.chat.invitelink.application;

public interface InviteLinkService {

    /** 예외: R001, R007 */
    InviteLinkResponse createLink(String userId, UUID roomId, CreateInviteLinkRequest request);

    /** 활성화된 링크(is_active=true)만 반환. 예외: R001, R007 */
    List<InviteLinkResponse> getLinks(String userId, UUID roomId);

    /**
     * 비밀번호 입력 생략 가능.
     * 이미 참여 중이면 에러 없이 200 반환.
     * 강퇴(kicked_at) 또는 자발 퇴장(left_at) 사용자도 재참여 가능 — R006 미적용.
     * GROUP 재참여: left_at=NULL, kicked_at=NULL 리셋 + profile_id 갱신 + 기본 그룹 재연결.
     * 예외: L001(만료), L002(비활성), R004, R009, P001, P002
     */
    InviteLinkJoinResponse joinByLink(String userId, String token, JoinByLinkRequest request);

    /** 예외: R001, R007 */
    void deactivateLink(String userId, UUID roomId, Long linkId);
}
```

### DTOs

```java
public record CreateInviteLinkRequest(
        OffsetDateTime expiresAt    // null = 만료 없음
) {}

public record JoinByLinkRequest(
        @NotNull Long profileId
) {}

public record InviteLinkResponse(
        Long id, String token, String inviteUrl,
        OffsetDateTime expiresAt, boolean isActive, OffsetDateTime createdAt
) {}

public record InviteLinkJoinResponse(
        UUID roomId, String type
) {}
```

---

## 6. MessageService

```java
package com.chat.message.application;

public interface MessageService {

    /**
     * DM: hidden_at 이전 메시지 자동 제외.
     * before null 이면 최신 메시지부터.
     * 예외: R001
     */
    MessageCursorResponse getMessages(String userId, UUID roomId, Long before, int size);

    /**
     * 반환값: StompChatHandler가 /topic/rooms/{roomId} 로 브로드캐스트할 payload.
     * 사이드이펙트: DM 방이면 상대 멤버의 is_hidden 자동 해제.
     */
    MessageBroadcastPayload sendMessage(String userId, UUID roomId, SendMessageRequest request);

    /**
     * 사이드이펙트: MESSAGE_EDITED 이벤트 내부 발행.
     * 예외: M001, M002, C001(유효성 오류)
     */
    void editMessage(String userId, Long messageId, EditMessageRequest request);

    /**
     * 소프트 딜리트 (deleted_at 마킹).
     * 사이드이펙트: MESSAGE_DELETED 이벤트 내부 발행.
     * 예외: M001, M002
     */
    void deleteMessage(String userId, Long messageId);

    /** 멱등. 예외: R001 */
    void markRead(String userId, UUID roomId, MarkReadRequest request);
}
```

### DTOs

```java
// Request
public record SendMessageRequest(
        @NotBlank @Size(max = 1000) String content,
        @NotNull MessageType type
) {}

public record EditMessageRequest(
        @NotBlank @Size(max = 1000) String content
) {}

public record MarkReadRequest(
        @NotNull Long lastReadMessageId
) {}

// Response
public record MessageResponse(
        Long id,
        String senderId,
        String senderNickname,  // 삭제된 프로필이면 빈 문자열
        String content,
        MessageType type,
        boolean isEdited,
        OffsetDateTime createdAt
) {}

public record MessageCursorResponse(
        List<MessageResponse> messages,
        boolean hasNext,
        Long nextCursor
) {}

// StompChatHandler → Redis pub 시 사용
public record MessageBroadcastPayload(
        String type,            // "MESSAGE"
        Long id,
        String senderId,
        String senderNickname,
        String content,
        MessageType messageType,
        boolean isEdited,
        OffsetDateTime createdAt
) {}
```

---

## 7. GroupService

```java
package com.chat.group.application;

public interface GroupService {
    List<GroupResponse> getMyGroups(String userId);

    /** 예외: G002(이름 중복), G003(최대 10개 초과) */
    GroupResponse createGroup(String userId, CreateGroupRequest request);

    /** 예외: G001, G002, G004(기본 그룹) */
    GroupResponse updateGroup(String userId, Long groupId, UpdateGroupRequest request);

    /** 예외: G001, G004(기본 그룹) */
    void deleteGroup(String userId, Long groupId);

    /** 이미 속해 있으면 에러 없이 그대로 반환 (idempotent). 예외: G001, R001 */
    void assignRoom(String userId, Long groupId, UUID roomId);

    /** 예외: G001, G004(기본 그룹), R001 */
    void removeRoom(String userId, Long groupId, UUID roomId);
}
```

### DTOs

```java
public record CreateGroupRequest(
        @NotBlank String name
) {}

public record UpdateGroupRequest(
        @NotBlank String name
) {}

public record GroupResponse(
        Long id, String name, boolean isDefault
) {}
```

---

## 8. PresenceService

```java
package com.chat.websocket.presence;

public interface PresenceService {
    /** Redis TTL 키 설정 (60초). STOMP CONNECT 및 heartbeat SEND 시 호출. */
    void heartbeat(String userId);

    boolean isOnline(String userId);

    /** Redis 키 즉시 삭제. STOMP DISCONNECT 시 호출. */
    void offline(String userId);
}
```

---

## WebSocket 브로드캐스트 Payload 요약

| 이벤트 | 발행 위치 | 채널 | 발행 조건 |
|--------|-----------|------|-----------|
| `MESSAGE` | `StompChatHandler` | `/topic/rooms/{roomId}` | WebSocket SEND |
| `MESSAGE_EDITED` | `MessageServiceImpl` | `/topic/rooms/{roomId}` | `PATCH /messages/{id}` |
| `MESSAGE_DELETED` | `MessageServiceImpl` | `/topic/rooms/{roomId}` | `DELETE /messages/{id}` |
| `OWNER_CHANGED` | `RoomServiceImpl` | `/topic/rooms/{roomId}` | 나가기 자동 위임 / 수동 위임 |
| `PROFILE_UPDATED` | `ProfileServiceImpl` | `/topic/rooms/{roomId}` (전체) | `PATCH /profiles/{id}` |
| `INVITATION` | `InvitationServiceImpl` | `/user/queue/notifications` | `POST /rooms/{roomId}/invitations` |
