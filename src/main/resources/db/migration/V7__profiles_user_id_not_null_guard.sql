-- profiles.user_id NOT NULL 강제
-- V5에서 nullable로 추가된 컬럼을 NOT NULL로 변경.
-- 대응하는 users row가 없는 고아 프로필은 먼저 제거.
DELETE FROM profiles WHERE user_id IS NULL OR user_id = '';
ALTER TABLE profiles ALTER COLUMN user_id SET NOT NULL;
