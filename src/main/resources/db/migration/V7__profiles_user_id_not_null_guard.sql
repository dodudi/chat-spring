-- V5 의 NOT NULL 제약은 빈 테이블에서만 안전하게 적용됨.
-- 운영 환경에서 user_id 없는 row가 남아있을 경우를 대비해 고아 row를 제거한다.
-- users 테이블에 대응하는 user_id가 없는 프로필은 더 이상 유효하지 않다.
DELETE FROM profiles WHERE user_id IS NULL OR user_id = '';
