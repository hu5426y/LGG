CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    username VARCHAR(32) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    student_no VARCHAR(64),
    college VARCHAR(64),
    class_name VARCHAR(64),
    avatar_url VARCHAR(255),
    gender VARCHAR(16),
    bio VARCHAR(255),
    role VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    total_duration_seconds INT NOT NULL DEFAULT 0,
    total_distance_km DOUBLE NOT NULL DEFAULT 0,
    points INT NOT NULL DEFAULT 0,
    level_value INT NOT NULL DEFAULT 1
);

CREATE TABLE banner (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    title VARCHAR(64) NOT NULL,
    subtitle VARCHAR(128),
    image_url VARCHAR(255) NOT NULL,
    link_type VARCHAR(32) NOT NULL,
    link_target VARCHAR(128),
    sort_order INT NOT NULL DEFAULT 0,
    active BIT NOT NULL DEFAULT b'1'
);

CREATE TABLE tutorial (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    title VARCHAR(128) NOT NULL,
    cover_url VARCHAR(255),
    summary VARCHAR(255),
    content LONGTEXT NOT NULL,
    published BIT NOT NULL DEFAULT b'1'
);

CREATE TABLE badge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    icon VARCHAR(255),
    rule_type VARCHAR(32) NOT NULL,
    rule_threshold INT NOT NULL,
    active BIT NOT NULL DEFAULT b'1'
);

CREATE TABLE level_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    level_number INT NOT NULL,
    title VARCHAR(64) NOT NULL,
    min_points INT NOT NULL
);

CREATE TABLE user_badge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    user_id BIGINT NOT NULL,
    badge_id BIGINT NOT NULL,
    granted_at DATETIME NOT NULL,
    CONSTRAINT fk_user_badge_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_user_badge_badge FOREIGN KEY (badge_id) REFERENCES badge(id),
    CONSTRAINT uk_user_badge UNIQUE (user_id, badge_id)
);

CREATE TABLE challenge_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    task_type VARCHAR(16) NOT NULL,
    goal_type VARCHAR(32) NOT NULL,
    goal_value INT NOT NULL,
    points_reward INT NOT NULL DEFAULT 0,
    active BIT NOT NULL DEFAULT b'1'
);

CREATE TABLE user_task_progress (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    user_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    current_value INT NOT NULL DEFAULT 0,
    completed BIT NOT NULL DEFAULT b'0',
    completed_at DATETIME NULL,
    CONSTRAINT fk_task_progress_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_task_progress_task FOREIGN KEY (task_id) REFERENCES challenge_task(id),
    CONSTRAINT uk_user_task UNIQUE (user_id, task_id)
);

CREATE TABLE run_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    user_id BIGINT NOT NULL,
    state VARCHAR(16) NOT NULL,
    started_at DATETIME NOT NULL,
    paused_at DATETIME NULL,
    finished_at DATETIME NULL,
    duration_seconds INT NOT NULL DEFAULT 0,
    distance_km DOUBLE NOT NULL DEFAULT 0,
    avg_pace_seconds INT NOT NULL DEFAULT 0,
    calories INT NOT NULL DEFAULT 0,
    step_count INT NOT NULL DEFAULT 0,
    route_snapshot LONGTEXT NULL,
    source VARCHAR(32),
    CONSTRAINT fk_run_session_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE club (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    name VARCHAR(64) NOT NULL,
    slogan VARCHAR(255),
    description VARCHAR(255),
    member_count INT NOT NULL DEFAULT 0,
    active BIT NOT NULL DEFAULT b'1'
);

CREATE TABLE club_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    club_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    CONSTRAINT fk_club_message_club FOREIGN KEY (club_id) REFERENCES club(id),
    CONSTRAINT fk_club_message_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE feed_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    user_id BIGINT NOT NULL,
    content LONGTEXT NOT NULL,
    image_urls VARCHAR(1000),
    like_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    report_count INT NOT NULL DEFAULT 0,
    risk_tags VARCHAR(255),
    featured BIT NOT NULL DEFAULT b'0',
    review_status VARCHAR(16) NOT NULL,
    CONSTRAINT fk_feed_post_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE post_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    CONSTRAINT fk_post_comment_post FOREIGN KEY (post_id) REFERENCES feed_post(id),
    CONSTRAINT fk_post_comment_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE post_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_post_like_post FOREIGN KEY (post_id) REFERENCES feed_post(id),
    CONSTRAINT fk_post_like_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT uk_post_like UNIQUE (post_id, user_id)
);

CREATE TABLE post_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    post_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    CONSTRAINT fk_post_report_post FOREIGN KEY (post_id) REFERENCES feed_post(id),
    CONSTRAINT fk_post_report_user FOREIGN KEY (reporter_id) REFERENCES sys_user(id)
);

CREATE TABLE activity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    title VARCHAR(128) NOT NULL,
    location VARCHAR(128),
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    registration_deadline DATETIME NOT NULL,
    cover_url VARCHAR(255),
    description LONGTEXT NOT NULL,
    max_capacity INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL
);

CREATE TABLE activity_registration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    activity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    CONSTRAINT fk_activity_registration_activity FOREIGN KEY (activity_id) REFERENCES activity(id),
    CONSTRAINT fk_activity_registration_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT uk_activity_user UNIQUE (activity_id, user_id)
);

CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    operator_id BIGINT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(64),
    detail VARCHAR(1000),
    CONSTRAINT fk_audit_log_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id)
);
