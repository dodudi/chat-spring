CREATE TABLE dm_channels (
    id         BIGSERIAL       PRIMARY KEY,
    type       dm_channel_type NOT NULL,
    name       VARCHAR(100),
    icon_url   VARCHAR(512),
    created_at TIMESTAMPTZ     NOT NULL DEFAULT now()
);

COMMENT ON TABLE  dm_channels          IS 'DM 채널. DIRECT=1:1, GROUP=그룹 DM';
COMMENT ON COLUMN dm_channels.name     IS '그룹 DM 이름. DIRECT 타입에서는 NULL';
COMMENT ON COLUMN dm_channels.icon_url IS '그룹 DM 아이콘. DIRECT 타입에서는 NULL';

CREATE TABLE dm_channel_participants (
    id            BIGSERIAL   PRIMARY KEY,
    dm_channel_id BIGINT      NOT NULL REFERENCES dm_channels (id) ON DELETE CASCADE,
    user_id       BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    joined_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at       TIMESTAMPTZ,
    CONSTRAINT uk_dm_channel_participants UNIQUE (dm_channel_id, user_id)
);

COMMENT ON TABLE  dm_channel_participants        IS 'DM 채널 참여자 목록';
COMMENT ON COLUMN dm_channel_participants.left_at IS '그룹 DM 나가기 일시. NULL이면 현재 참여 중';

CREATE TABLE dm_messages (
    id                BIGSERIAL   PRIMARY KEY,
    dm_channel_id     BIGINT      NOT NULL REFERENCES dm_channels (id) ON DELETE CASCADE,
    sender_id         BIGINT      NOT NULL REFERENCES users (id),
    content           TEXT,
    parent_message_id BIGINT      REFERENCES dm_messages (id),
    is_edited         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ
);

COMMENT ON TABLE  dm_messages                   IS 'DM 채널 메시지';
COMMENT ON COLUMN dm_messages.parent_message_id IS '답장 대상 메시지. NULL이면 일반 메시지';
COMMENT ON COLUMN dm_messages.deleted_at        IS '소프트 삭제 일시. NULL이면 삭제되지 않은 메시지';

CREATE INDEX idx_dm_messages_channel_created ON dm_messages (dm_channel_id, created_at DESC);

CREATE TABLE dm_message_attachments (
    id            BIGSERIAL     PRIMARY KEY,
    dm_message_id BIGINT        NOT NULL REFERENCES dm_messages (id) ON DELETE CASCADE,
    file_name     VARCHAR(255)  NOT NULL,
    file_url      VARCHAR(1024) NOT NULL,
    file_size     BIGINT        NOT NULL,
    content_type  VARCHAR(127)  NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

COMMENT ON TABLE  dm_message_attachments              IS 'DM 메시지에 첨부된 파일';
COMMENT ON COLUMN dm_message_attachments.file_size    IS '파일 크기 (바이트)';
COMMENT ON COLUMN dm_message_attachments.content_type IS 'MIME 타입 (예: image/png, application/pdf)';

CREATE TABLE dm_read_status (
    id                   BIGSERIAL   PRIMARY KEY,
    dm_channel_id        BIGINT      NOT NULL REFERENCES dm_channels (id) ON DELETE CASCADE,
    user_id              BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    last_read_message_id BIGINT      NOT NULL REFERENCES dm_messages (id),
    last_read_at         TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_dm_read_status UNIQUE (dm_channel_id, user_id)
);

COMMENT ON TABLE  dm_read_status                      IS 'DM 채널별 마지막 읽은 메시지 추적';
COMMENT ON COLUMN dm_read_status.last_read_message_id IS '마지막으로 읽은 메시지. 이 id 이후 메시지가 읽지 않은 메시지';
