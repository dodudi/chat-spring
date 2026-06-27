CREATE TABLE users (
    id          BIGSERIAL    PRIMARY KEY,
    external_id VARCHAR(255) NOT NULL,
    status      user_status  NOT NULL DEFAULT 'OFFLINE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_external_id UNIQUE (external_id)
);

COMMENT ON TABLE  users             IS '채팅 서버 내 사용자. 인증은 외부 OAuth2 서버가 담당하며 JWT sub 클레임으로 식별한다';
COMMENT ON COLUMN users.external_id IS 'JWT sub 클레임. 외부 인증 서버의 사용자 식별자';
COMMENT ON COLUMN users.status      IS '실시간 접속 상태. 앱 연결·해제 시 WebSocket 이벤트로 갱신';

CREATE TABLE user_blocks (
    id         BIGSERIAL   PRIMARY KEY,
    blocker_id BIGINT      NOT NULL REFERENCES users (id),
    blocked_id BIGINT      NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_user_blocks           UNIQUE (blocker_id, blocked_id),
    CONSTRAINT chk_user_blocks_not_self CHECK (blocker_id <> blocked_id)
);

COMMENT ON TABLE  user_blocks            IS '사용자 차단 관계. 차단 시 메시지 수신·친구 요청·DM을 차단한다';
COMMENT ON COLUMN user_blocks.blocker_id IS '차단한 사용자';
COMMENT ON COLUMN user_blocks.blocked_id IS '차단된 사용자';
