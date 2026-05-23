-- chat_room_members.profile_id: NOT NULL 강제
-- 채팅방 입장 시 항상 profileId를 선택하므로 NULL 불허
-- profile_id 없는 고아 멤버 row를 먼저 제거한 후 NOT NULL 적용
DELETE FROM chat_room_members WHERE profile_id IS NULL;
ALTER TABLE chat_room_members ALTER COLUMN profile_id SET NOT NULL;

-- messages.profile_id: ON DELETE SET NULL 변경
-- 프로필 삭제 시 과거 메시지 닉네임은 빈칸으로 표시 (메시지는 sender_id로 식별)
ALTER TABLE messages DROP CONSTRAINT IF EXISTS messages_profile_id_fkey;
ALTER TABLE messages ADD CONSTRAINT messages_profile_id_fkey
    FOREIGN KEY (profile_id) REFERENCES profiles (id) ON DELETE SET NULL;
