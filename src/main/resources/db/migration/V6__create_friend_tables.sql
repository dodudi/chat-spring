CREATE TABLE friend_requests (
    id           BIGSERIAL             PRIMARY KEY,
    requester_id BIGINT                NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    receiver_id  BIGINT                NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status       friend_request_status NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ           NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ           NOT NULL DEFAULT now(),
    CONSTRAINT uk_friend_requests           UNIQUE (requester_id, receiver_id),
    CONSTRAINT chk_friend_requests_not_self CHECK (requester_id <> receiver_id)
);

COMMENT ON TABLE  friend_requests             IS '친구 요청 이력. ACCEPTED로 바뀌면 friendships에 레코드를 생성한다';
COMMENT ON COLUMN friend_requests.requester_id IS '친구 요청을 보낸 사용자';
COMMENT ON COLUMN friend_requests.receiver_id  IS '친구 요청을 받은 사용자';

CREATE TABLE friendships (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    friend_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_friendships        UNIQUE (user_id, friend_id),
    CONSTRAINT chk_friendships_order CHECK (user_id < friend_id)
);

COMMENT ON TABLE  friendships           IS '친구 관계. user_id < friend_id 제약으로 (A,B)와 (B,A) 중복 저장을 방지한다';
COMMENT ON COLUMN friendships.user_id   IS '두 사용자 중 id가 작은 쪽';
COMMENT ON COLUMN friendships.friend_id IS '두 사용자 중 id가 큰 쪽';
