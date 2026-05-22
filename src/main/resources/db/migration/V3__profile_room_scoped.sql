-- 프로필을 채팅방 종속으로 변경
-- 프로필은 회원이 아닌 채팅방 입장 시 생성되므로 user_id 및 관련 제약 제거
DROP INDEX IF EXISTS idx_profiles_user_id;
ALTER TABLE profiles DROP CONSTRAINT IF EXISTS uidx_profiles_nickname;
ALTER TABLE profiles DROP COLUMN user_id;
