CREATE TABLE chat_room_members (
    id                   BIGINT       PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    room_id              BIGINT       NOT NULL REFERENCES chat_rooms (id),
    user_id              VARCHAR(255) NOT NULL,
    joined_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_read_message_id BIGINT,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_members_room_id ON chat_room_members (room_id);
CREATE INDEX idx_members_user_id ON chat_room_members (user_id);

-- 한 사용자가 동일 방에 중복 가입 방지
CREATE UNIQUE INDEX uidx_members_room_user ON chat_room_members (room_id, user_id);
