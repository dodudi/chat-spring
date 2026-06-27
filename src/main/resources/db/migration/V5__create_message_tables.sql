CREATE TABLE messages (
    id                BIGSERIAL    PRIMARY KEY,
    channel_id        BIGINT       NOT NULL REFERENCES channels (id) ON DELETE CASCADE,
    sender_id         BIGINT       NOT NULL REFERENCES users (id),
    content           TEXT,
    type              message_type NOT NULL DEFAULT 'DEFAULT',
    parent_message_id BIGINT       REFERENCES messages (id),
    is_edited         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ
);

COMMENT ON TABLE  messages                   IS '채널 메시지. content와 첨부파일이 모두 없는 메시지는 허용하지 않는다';
COMMENT ON COLUMN messages.content           IS '텍스트 내용. 첨부파일만 있는 경우 NULL 가능';
COMMENT ON COLUMN messages.parent_message_id IS 'REPLY 타입일 때 답장 대상 메시지. NULL이면 일반 메시지';
COMMENT ON COLUMN messages.deleted_at        IS '소프트 삭제 일시. NULL이면 삭제되지 않은 메시지';

CREATE INDEX idx_messages_channel_created ON messages (channel_id, created_at DESC);

CREATE TABLE message_attachments (
    id           BIGSERIAL     PRIMARY KEY,
    message_id   BIGINT        NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    file_name    VARCHAR(255)  NOT NULL,
    file_url     VARCHAR(1024) NOT NULL,
    file_size    BIGINT        NOT NULL,
    content_type VARCHAR(127)  NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

COMMENT ON TABLE  message_attachments              IS '메시지에 첨부된 파일';
COMMENT ON COLUMN message_attachments.file_size    IS '파일 크기 (바이트)';
COMMENT ON COLUMN message_attachments.content_type IS 'MIME 타입 (예: image/png, application/pdf)';

CREATE TABLE message_reactions (
    id         BIGSERIAL   PRIMARY KEY,
    message_id BIGINT      NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    emoji      VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_message_reactions UNIQUE (message_id, user_id, emoji)
);

COMMENT ON TABLE  message_reactions       IS '메시지 이모지 반응. 동일 이모지는 사용자당 1회만 허용';
COMMENT ON COLUMN message_reactions.emoji IS '유니코드 이모지 문자열 또는 커스텀 이모지 ID';

CREATE TABLE message_read_status (
    id                   BIGSERIAL   PRIMARY KEY,
    channel_id           BIGINT      NOT NULL REFERENCES channels (id) ON DELETE CASCADE,
    user_id              BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    last_read_message_id BIGINT      NOT NULL REFERENCES messages (id),
    last_read_at         TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_message_read_status UNIQUE (channel_id, user_id)
);

COMMENT ON TABLE  message_read_status                      IS '채널별 마지막 읽은 메시지 추적. 읽지 않은 메시지 수 계산에 사용';
COMMENT ON COLUMN message_read_status.last_read_message_id IS '마지막으로 읽은 메시지. 이 id 이후 메시지가 읽지 않은 메시지';

CREATE TABLE pinned_messages (
    id         BIGSERIAL   PRIMARY KEY,
    channel_id BIGINT      NOT NULL REFERENCES channels (id) ON DELETE CASCADE,
    message_id BIGINT      NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    pinned_by  BIGINT      NOT NULL REFERENCES users (id),
    pinned_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_pinned_messages UNIQUE (channel_id, message_id)
);

COMMENT ON TABLE  pinned_messages           IS '채널에 고정된 메시지 목록';
COMMENT ON COLUMN pinned_messages.pinned_by IS '핀을 고정한 사용자';
