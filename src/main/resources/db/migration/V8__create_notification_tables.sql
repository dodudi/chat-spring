CREATE TABLE notifications (
    id             BIGSERIAL         PRIMARY KEY,
    user_id        BIGINT            NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type           notification_type NOT NULL,
    reference_type VARCHAR(32)       NOT NULL,
    reference_id   BIGINT            NOT NULL,
    is_read        BOOLEAN           NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ       NOT NULL DEFAULT now()
);

COMMENT ON TABLE  notifications                IS '사용자 알림';
COMMENT ON COLUMN notifications.reference_type IS '알림 원본 종류. message / friend_request / server_invite 등';
COMMENT ON COLUMN notifications.reference_id   IS 'reference_type에 해당하는 레코드 id';
COMMENT ON COLUMN notifications.is_read        IS 'TRUE이면 읽은 알림';

CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read, created_at DESC);

CREATE TABLE notification_settings (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    server_id   BIGINT      REFERENCES servers (id) ON DELETE CASCADE,
    channel_id  BIGINT      REFERENCES channels (id) ON DELETE CASCADE,
    mute_level  mute_level  NOT NULL DEFAULT 'ALL',
    muted_until TIMESTAMPTZ,
    CONSTRAINT uk_notification_settings UNIQUE (user_id, server_id, channel_id)
);

COMMENT ON TABLE  notification_settings            IS '알림 음소거 설정. server_id·channel_id 모두 NULL이면 전역 설정';
COMMENT ON COLUMN notification_settings.server_id  IS 'NULL이면 전역 또는 채널 단위 설정';
COMMENT ON COLUMN notification_settings.channel_id IS 'NULL이면 전역 또는 서버 단위 설정';
COMMENT ON COLUMN notification_settings.muted_until IS '임시 음소거 해제 일시. NULL이면 영구 적용';
