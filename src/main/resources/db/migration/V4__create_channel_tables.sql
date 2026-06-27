CREATE TABLE channel_categories (
    id         BIGSERIAL    PRIMARY KEY,
    server_id  BIGINT       NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    position   INT          NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE  channel_categories          IS '채널을 묶는 카테고리. 카테고리 없이 채널만 배치할 수도 있다';
COMMENT ON COLUMN channel_categories.position IS '서버 내 카테고리 표시 순서';

CREATE TABLE channels (
    id               BIGSERIAL    PRIMARY KEY,
    server_id        BIGINT       NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    category_id      BIGINT       REFERENCES channel_categories (id) ON DELETE SET NULL,
    type             channel_type NOT NULL,
    name             VARCHAR(100) NOT NULL,
    description      VARCHAR(1024),
    position         INT          NOT NULL,
    is_nsfw          BOOLEAN      NOT NULL DEFAULT FALSE,
    slowmode_seconds INT          NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE  channels                  IS '서버 채널. 카테고리 없이 독립 배치 가능 (category_id NULL)';
COMMENT ON COLUMN channels.position         IS '카테고리 내 채널 표시 순서';
COMMENT ON COLUMN channels.slowmode_seconds IS '메시지 전송 간격 제한(초). 0이면 슬로우모드 비활성';

CREATE TABLE channel_permission_overwrites (
    id                BIGSERIAL              PRIMARY KEY,
    channel_id        BIGINT                 NOT NULL REFERENCES channels (id) ON DELETE CASCADE,
    target_type       permission_target_type NOT NULL,
    target_id         BIGINT                 NOT NULL,
    allow_permissions BIGINT                 NOT NULL DEFAULT 0,
    deny_permissions  BIGINT                 NOT NULL DEFAULT 0,
    CONSTRAINT uk_channel_permission_overwrites UNIQUE (channel_id, target_type, target_id)
);

COMMENT ON TABLE  channel_permission_overwrites                  IS '역할 또는 특정 멤버의 채널 권한을 기본 역할 권한 위에 덮어씀';
COMMENT ON COLUMN channel_permission_overwrites.target_id        IS 'target_type이 ROLE이면 server_roles.id, MEMBER이면 server_members.id';
COMMENT ON COLUMN channel_permission_overwrites.allow_permissions IS '기본 권한에서 명시적으로 허용할 비트필드';
COMMENT ON COLUMN channel_permission_overwrites.deny_permissions  IS '기본 권한에서 명시적으로 거부할 비트필드';
