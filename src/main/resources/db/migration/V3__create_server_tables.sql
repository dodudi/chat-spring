CREATE TABLE servers (
    id          BIGSERIAL    PRIMARY KEY,
    owner_id    BIGINT       NOT NULL REFERENCES users (id),
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    icon_url    VARCHAR(512),
    invite_code VARCHAR(16),
    is_public   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_servers_invite_code UNIQUE (invite_code)
);

COMMENT ON TABLE  servers             IS 'Discord의 길드(Guild)에 해당하는 커뮤니티 단위';
COMMENT ON COLUMN servers.invite_code IS '공개 초대 코드. NULL이면 초대 링크 없음';
COMMENT ON COLUMN servers.is_public   IS 'TRUE이면 초대 코드 없이 검색·참여 가능';

CREATE TABLE server_roles (
    id             BIGSERIAL   PRIMARY KEY,
    server_id      BIGINT      NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    name           VARCHAR(64) NOT NULL,
    color          VARCHAR(7),
    position       INT         NOT NULL,
    permissions    BIGINT      NOT NULL DEFAULT 0,
    is_mentionable BOOLEAN     NOT NULL DEFAULT TRUE,
    is_hoist       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE  server_roles             IS '서버 역할. position이 높을수록 상위 역할';
COMMENT ON COLUMN server_roles.color       IS '역할 색상 hex 코드 (#RRGGBB). NULL이면 기본 색상';
COMMENT ON COLUMN server_roles.position    IS '역할 우선순위. 높을수록 상위 역할';
COMMENT ON COLUMN server_roles.permissions IS '허용 권한 비트필드. 각 비트가 특정 권한을 나타냄';
COMMENT ON COLUMN server_roles.is_hoist    IS 'TRUE이면 멤버 목록에서 역할별로 분리 표시';

CREATE TABLE server_members (
    id        BIGSERIAL   PRIMARY KEY,
    server_id BIGINT      NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    user_id   BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    nickname  VARCHAR(32),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_server_members UNIQUE (server_id, user_id)
);

COMMENT ON TABLE  server_members          IS '서버 참여 멤버';
COMMENT ON COLUMN server_members.nickname IS '서버 내 별명. NULL이면 users.display_name 사용';

CREATE TABLE server_member_roles (
    server_member_id BIGINT NOT NULL REFERENCES server_members (id) ON DELETE CASCADE,
    role_id          BIGINT NOT NULL REFERENCES server_roles (id) ON DELETE CASCADE,
    PRIMARY KEY (server_member_id, role_id)
);

COMMENT ON TABLE server_member_roles IS '멤버에게 부여된 역할 목록';

CREATE TABLE server_invites (
    id         BIGSERIAL   PRIMARY KEY,
    server_id  BIGINT      NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    created_by BIGINT      NOT NULL REFERENCES users (id),
    code       VARCHAR(16) NOT NULL,
    max_uses   INT,
    uses       INT         NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_server_invites_code UNIQUE (code)
);

COMMENT ON TABLE  server_invites            IS '서버 초대 링크';
COMMENT ON COLUMN server_invites.max_uses   IS '최대 사용 횟수. NULL이면 무제한';
COMMENT ON COLUMN server_invites.expires_at IS '만료 일시. NULL이면 만료 없음';
