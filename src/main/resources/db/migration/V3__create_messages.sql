CREATE TABLE messages (
    id         BIGINT       PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    room_id    BIGINT       NOT NULL REFERENCES chat_rooms (id),
    sender_id  VARCHAR(255) NOT NULL,
    content    TEXT         NOT NULL,
    type       VARCHAR(50)  NOT NULL DEFAULT 'TEXT',
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_messages_type CHECK (type IN ('TEXT', 'IMAGE', 'FILE'))
);

-- 채팅방 메시지 히스토리 커서 페이징 (최신순)
CREATE INDEX idx_messages_room_created ON messages (room_id, created_at DESC);
CREATE INDEX idx_messages_sender_id ON messages (sender_id);
