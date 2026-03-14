ALTER TABLE sys_user
    ADD COLUMN wechat_openid VARCHAR(128) NULL,
    ADD COLUMN wechat_unionid VARCHAR(128) NULL,
    ADD COLUMN wechat_bound_at DATETIME NULL;

ALTER TABLE run_session
    ADD COLUMN route_point_count INT NOT NULL DEFAULT 0,
    ADD COLUMN device_model VARCHAR(64) NULL,
    ADD COLUMN device_platform VARCHAR(32) NULL,
    ADD COLUMN client_version VARCHAR(32) NULL;

CREATE UNIQUE INDEX uk_sys_user_student_no ON sys_user (student_no);
CREATE UNIQUE INDEX uk_sys_user_wechat_openid ON sys_user (wechat_openid);
CREATE INDEX idx_run_session_user_state_started_at ON run_session (user_id, state, started_at DESC);
CREATE INDEX idx_run_session_user_started_at ON run_session (user_id, started_at DESC);
