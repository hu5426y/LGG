ALTER TABLE run_session
    ADD COLUMN cloud_session_id VARCHAR(64) NULL,
    ADD COLUMN motion_confidence DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN sensor_summary_json LONGTEXT NULL;

CREATE INDEX idx_run_session_finished_at ON run_session (finished_at);
CREATE INDEX idx_run_session_cloud_session ON run_session (cloud_session_id);

CREATE TABLE run_plan_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    code VARCHAR(32) NOT NULL UNIQUE,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    plan_type VARCHAR(32) NOT NULL,
    duration_days INT NOT NULL,
    target_distance_km DOUBLE NOT NULL DEFAULT 0,
    target_runs INT NOT NULL DEFAULT 0,
    active BIT NOT NULL DEFAULT b'1'
);

CREATE TABLE user_run_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    user_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    started_on DATE NOT NULL,
    completed_on DATE NULL,
    current_day_index INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_user_run_plan_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_user_run_plan_template FOREIGN KEY (template_id) REFERENCES run_plan_template(id)
);

CREATE INDEX idx_user_run_plan_user_status ON user_run_plan (user_id, status);

CREATE TABLE user_run_plan_day (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    user_run_plan_id BIGINT NOT NULL,
    day_index INT NOT NULL,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    target_distance_km DOUBLE NOT NULL DEFAULT 0,
    target_duration_minutes INT NOT NULL DEFAULT 0,
    completed BIT NOT NULL DEFAULT b'0',
    completed_on DATE NULL,
    source_run_id BIGINT NULL,
    CONSTRAINT fk_user_run_plan_day_plan FOREIGN KEY (user_run_plan_id) REFERENCES user_run_plan(id),
    CONSTRAINT fk_user_run_plan_day_run FOREIGN KEY (source_run_id) REFERENCES run_session(id),
    CONSTRAINT uk_user_run_plan_day UNIQUE (user_run_plan_id, day_index)
);

CREATE TABLE daily_checkin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    user_id BIGINT NOT NULL,
    checkin_date DATE NOT NULL,
    source_run_id BIGINT NOT NULL,
    plan_day_id BIGINT NULL,
    streak_days INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_daily_checkin_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_daily_checkin_run FOREIGN KEY (source_run_id) REFERENCES run_session(id),
    CONSTRAINT fk_daily_checkin_plan_day FOREIGN KEY (plan_day_id) REFERENCES user_run_plan_day(id),
    CONSTRAINT uk_daily_checkin_user_date UNIQUE (user_id, checkin_date)
);

CREATE TABLE club_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    club_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    active BIT NOT NULL DEFAULT b'1',
    joined_at DATETIME NOT NULL,
    CONSTRAINT fk_club_member_club FOREIGN KEY (club_id) REFERENCES club(id),
    CONSTRAINT fk_club_member_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT uk_club_member UNIQUE (club_id, user_id)
);

CREATE INDEX idx_club_member_user_active ON club_member (user_id, active);

CREATE TABLE run_daily_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    stat_date DATE NOT NULL,
    active_users INT NOT NULL DEFAULT 0,
    total_distance_km DOUBLE NOT NULL DEFAULT 0,
    total_duration_seconds INT NOT NULL DEFAULT 0,
    average_pace_seconds INT NOT NULL DEFAULT 0,
    completed_plans INT NOT NULL DEFAULT 0,
    checkin_users INT NOT NULL DEFAULT 0,
    active_squads INT NOT NULL DEFAULT 0,
    squad_message_count INT NOT NULL DEFAULT 0,
    CONSTRAINT uk_run_daily_stats_date UNIQUE (stat_date)
);

INSERT INTO run_plan_template (created_at, updated_at, code, title, description, plan_type, duration_days, target_distance_km, target_runs, active)
VALUES
    (NOW(), NOW(), 'BEGINNER_5K', '5km 入门', '从轻松跑到稳定完成 5km 的入门计划。', 'DISTANCE', 14, 5.0, 6, b'1'),
    (NOW(), NOW(), 'FAT_BURN', '减脂跑', '以稳定频率建立每周跑步习惯。', 'FREQUENCY', 7, 3.0, 4, b'1'),
    (NOW(), NOW(), 'WEEKLY_GOAL', '周目标', '连续 7 天维持日常跑步和打卡节奏。', 'MIXED', 7, 2.5, 5, b'1');
