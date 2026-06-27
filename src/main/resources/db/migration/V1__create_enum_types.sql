CREATE TYPE user_status AS ENUM ('ONLINE', 'OFFLINE', 'IDLE', 'DND');
CREATE TYPE channel_type AS ENUM ('TEXT', 'VOICE', 'ANNOUNCEMENT', 'FORUM');
CREATE TYPE message_type AS ENUM ('DEFAULT', 'REPLY', 'SYSTEM');
CREATE TYPE dm_channel_type AS ENUM ('DIRECT', 'GROUP');
CREATE TYPE permission_target_type AS ENUM ('ROLE', 'MEMBER');
CREATE TYPE friend_request_status AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED');
CREATE TYPE notification_type AS ENUM ('FRIEND_REQUEST', 'SERVER_INVITE', 'MENTION', 'REPLY', 'REACTION');
CREATE TYPE mute_level AS ENUM ('ALL', 'MENTIONS_ONLY', 'NOTHING');

COMMENT ON TYPE user_status IS '사용자 접속 상태. ONLINE=활동 중, OFFLINE=오프라인, IDLE=자리 비움(비활동), DND=방해 금지';
COMMENT ON TYPE channel_type IS '채널 종류. TEXT=텍스트, VOICE=음성, ANNOUNCEMENT=공지, FORUM=포럼';
COMMENT ON TYPE message_type IS '메시지 종류. DEFAULT=일반, REPLY=답장, SYSTEM=시스템 메시지';
COMMENT ON TYPE dm_channel_type IS 'DM 채널 종류. DIRECT=1:1, GROUP=그룹 DM';
COMMENT ON TYPE permission_target_type IS '채널 권한 덮어쓰기 대상. ROLE=역할, MEMBER=특정 멤버';
COMMENT ON TYPE friend_request_status IS '친구 요청 상태. PENDING=대기, ACCEPTED=수락, REJECTED=거절, CANCELLED=취소';
COMMENT ON TYPE notification_type IS '알림 종류';
COMMENT ON TYPE mute_level IS '알림 음소거 수준. ALL=모두 허용, MENTIONS_ONLY=멘션만, NOTHING=모두 차단';
