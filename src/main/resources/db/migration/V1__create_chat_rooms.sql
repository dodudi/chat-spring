-- =============================================
-- 채팅방
-- =============================================
CREATE TABLE chat_rooms
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    type       VARCHAR(50)  NOT NULL,
    room_key   VARCHAR(255) NOT NULL,
    name       VARCHAR(100),
    password   VARCHAR(255),
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rooms_type CHECK (type IN ('DM', 'GROUP', 'PUBLIC')),
    CONSTRAINT uidx_rooms_room_key UNIQUE (room_key)
);

CREATE INDEX idx_chat_rooms_created_by ON chat_rooms (created_by);

-- =============================================
-- 프로필 (사용자당 여러 개, 최초 로그인 시 기본 1개 자동 생성)
-- =============================================
CREATE TABLE profiles
(
    id         BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id    VARCHAR(255) NOT NULL,
    nickname   VARCHAR(50)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profiles_user_id ON profiles (user_id);

-- =============================================
-- 채팅방 참여자
--
-- [DM]
--   is_hidden  : 현재 목록에서 숨김 여부
--   hidden_at  : 숨김 처리 시각 — 메시지 노출 기준점. 숨김 해제 후에도 유지.
--
-- [GROUP]
--   left_at    : 자발적 퇴장 시각 — NOT NULL 이면 현재 미참여 상태 (초대 받으면 재참여 가능)
--   kicked_at  : 강퇴 시각 — NOT NULL 이면 재참여·초대 불가
--
-- [PUBLIC]
--   kicked_at  : 강퇴 시각 — NOT NULL 이면 재참여·초대 불가
--   (자발적 퇴장 시 레코드 삭제, 재참여 시 새 레코드 생성)
-- =============================================
CREATE TABLE chat_room_members
(
    id         BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    room_id    UUID         NOT NULL REFERENCES chat_rooms (id) ON DELETE CASCADE,
    user_id    VARCHAR(255) NOT NULL,
    profile_id BIGINT REFERENCES profiles (id),
    role       VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
    is_hidden  BOOLEAN      NOT NULL DEFAULT false,
    hidden_at  TIMESTAMPTZ,
    left_at    TIMESTAMPTZ,
    kicked_at  TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_members_role CHECK (role IN ('OWNER', 'MEMBER')),
    CONSTRAINT uidx_members_room_user UNIQUE (room_id, user_id)
);

CREATE INDEX idx_members_room_id ON chat_room_members (room_id);
CREATE INDEX idx_members_user_id ON chat_room_members (user_id);

-- =============================================
-- 초대
-- =============================================
CREATE TABLE invitations
(
    id         BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    room_id    UUID         NOT NULL REFERENCES chat_rooms (id) ON DELETE CASCADE,
    inviter_id VARCHAR(255) NOT NULL,
    invitee_id VARCHAR(255) NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_invitations_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED'))
);

CREATE INDEX idx_invitations_invitee_id ON invitations (invitee_id);
CREATE INDEX idx_invitations_room_id ON invitations (room_id);
CREATE UNIQUE INDEX uidx_invitations_pending ON invitations (room_id, invitee_id) WHERE status = 'PENDING';

-- =============================================
-- 메시지
-- =============================================
CREATE TABLE messages
(
    id         BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    room_id    UUID         NOT NULL REFERENCES chat_rooms (id) ON DELETE CASCADE,
    sender_id  VARCHAR(255) NOT NULL,
    profile_id BIGINT REFERENCES profiles (id),
    content    TEXT         NOT NULL,
    type       VARCHAR(20)  NOT NULL DEFAULT 'TEXT',
    is_edited  BOOLEAN      NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_messages_type CHECK (type IN ('TEXT', 'IMAGE', 'FILE'))
);

CREATE INDEX idx_messages_room_id ON messages (room_id);
CREATE INDEX idx_messages_created_at ON messages (room_id, created_at);

-- =============================================
-- 읽음 커서 (채팅방별 마지막 읽은 메시지)
-- =============================================
CREATE TABLE room_read_cursors
(
    id                   BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    room_id              UUID         NOT NULL REFERENCES chat_rooms (id) ON DELETE CASCADE,
    user_id              VARCHAR(255) NOT NULL,
    last_read_message_id BIGINT       REFERENCES messages (id) ON DELETE SET NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uidx_read_cursors_room_user UNIQUE (room_id, user_id)
);

-- =============================================
-- 사용자 그룹 (채팅방 분류용, 기본 그룹 포함)
-- =============================================
CREATE TABLE user_groups
(
    id         BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id    VARCHAR(255) NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    is_default BOOLEAN      NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uidx_groups_user_name UNIQUE (user_id, name)
);

CREATE INDEX idx_user_groups_user_id ON user_groups (user_id);

-- =============================================
-- 채팅방-그룹 연결
-- =============================================
CREATE TABLE room_group_memberships
(
    id         BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    room_id    UUID        NOT NULL REFERENCES chat_rooms (id) ON DELETE CASCADE,
    group_id   BIGINT      NOT NULL REFERENCES user_groups (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uidx_room_group UNIQUE (room_id, group_id)
);

-- =============================================
-- 채팅방 초대 URI
--
-- token      : URI에 포함될 고유 토큰
-- expires_at : NULL이면 만료 없음. NOT NULL이면 해당 시각 이후 사용 불가
-- is_active  : false이면 방장이 비활성화(삭제)한 URI
-- =============================================
CREATE TABLE room_invite_links
(
    id         BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    room_id    UUID         NOT NULL REFERENCES chat_rooms (id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ,
    is_active  BOOLEAN      NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uidx_invite_links_token UNIQUE (token)
);

CREATE INDEX idx_invite_links_room_id ON room_invite_links (room_id);