-- 프로필을 사용자 종속으로 변경
-- 채팅방 입장 시 profileId를 선택하는 방식으로 재설계.
-- 최초 API 호출 시 기본 프로필 1개 자동 생성.
ALTER TABLE profiles ADD COLUMN user_id VARCHAR(255) NOT NULL;

CREATE INDEX idx_profiles_user_id ON profiles (user_id);
