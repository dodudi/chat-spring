-- 프로필 닉네임 서비스 전체 고유 제약
ALTER TABLE profiles
    ADD CONSTRAINT uidx_profiles_nickname UNIQUE (nickname);
