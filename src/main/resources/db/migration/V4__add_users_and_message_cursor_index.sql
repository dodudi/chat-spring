-- =============================================
-- 사용자 테이블
-- 최초 API 호출 시 JWT sub 기준으로 upsert.
-- targetUserId 유효성 검증(DM 생성 등) 및 기본 그룹 자동 생성에 사용.
-- =============================================
CREATE TABLE users
(
    id         VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================
-- 메시지 커서 페이징 인덱스
-- GET /rooms/{roomId}/messages?before={messageId} 쿼리 최적화.
-- 기존 (room_id, created_at) 인덱스는 created_at 정렬 용도로 유지.
-- =============================================
CREATE INDEX idx_messages_room_id_cursor ON messages (room_id, id DESC) WHERE deleted_at IS NULL;
