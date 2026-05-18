CREATE TABLE chat_rooms (
    id         BIGINT       PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    type       VARCHAR(50)  NOT NULL,
    name       VARCHAR(100),
    created_by VARCHAR(255) NOT NULL,
    dm_user_a  VARCHAR(255),
    dm_user_b  VARCHAR(255),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rooms_type CHECK (type IN ('DM', 'GROUP'))
);

CREATE INDEX idx_chat_rooms_created_by ON chat_rooms (created_by);

-- DM 방 중복 생성 방지: 두 참여자 ID를 항상 정렬된 순서로 저장해 유니크 보장
CREATE UNIQUE INDEX uidx_chat_rooms_dm ON chat_rooms (dm_user_a, dm_user_b) WHERE type = 'DM';
