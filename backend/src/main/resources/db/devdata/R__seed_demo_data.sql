INSERT INTO sys_user (id, created_at, updated_at, username, password, display_name, student_no, college, class_name, avatar_url, gender, bio, role, status, total_duration_seconds, total_distance_km, points, level_value)
VALUES
    (1, NOW(), NOW(), 'admin', '{noop}admin123', '系统管理员', NULL, '体育学院', '管理组', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e', '男', '负责内容运营与审核', 'ADMIN', 'ACTIVE', 0, 0, 0, 1),
    (2, NOW(), NOW(), '20230001', '{noop}123456', '张晨', '20230001', '计算机学院', '计科 2301', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330', '女', '每周目标 15 公里', 'STUDENT', 'ACTIVE', 8520, 36.5, 320, 3),
    (3, NOW(), NOW(), '20230002', '{noop}123456', '李浩', '20230002', '外国语学院', '英语 2302', 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d', '男', '喜欢夜跑和长距离', 'STUDENT', 'ACTIVE', 9640, 42.8, 410, 4),
    (4, NOW(), NOW(), '20230003', '{noop}123456', '王璇', '20230003', '经济学院', '金融 2301', 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80', '女', '本学期目标完成 5km', 'STUDENT', 'ACTIVE', 7020, 29.4, 260, 2)
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO banner (id, created_at, updated_at, title, subtitle, image_url, link_type, link_target, sort_order, active)
VALUES
    (1, NOW(), NOW(), '本周校园跑挑战', '累计 12 公里解锁限时徽章', 'https://images.unsplash.com/photo-1552674605-db6ffd4facb5', 'TASK', 'weekly', 1, b'1'),
    (2, NOW(), NOW(), '春季环湖夜跑', '周六 19:00 南门集合', 'https://images.unsplash.com/photo-1486218119243-13883505764c', 'ACTIVITY', '1', 2, b'1'),
    (3, NOW(), NOW(), '热搜动态精选', '看看今天谁在霸榜', 'https://images.unsplash.com/photo-1517836357463-d25dfeac3438', 'POST', '1', 3, b'1')
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO tutorial (id, created_at, updated_at, title, cover_url, summary, content, published)
VALUES
    (1, NOW(), NOW(), '5km 新手训练法', 'https://images.unsplash.com/photo-1476480862126-209bfaa8edc8', '四周完成从 0 到 5km 的递进计划。', '建议按照慢跑+快走交替方式，每周训练 3 到 4 次，循序渐进。', b'1'),
    (2, NOW(), NOW(), '跑前热身与跑后拉伸', 'https://images.unsplash.com/photo-1518611012118-696072aa579a', '避免受伤的基础动作清单。', '包含髋部激活、小腿拉伸、股四头肌放松等内容。', b'1')
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO badge (id, created_at, updated_at, code, name, description, icon, rule_type, rule_threshold, active)
VALUES
    (1, NOW(), NOW(), 'FIRST_RUN', '首跑达人', '完成 1 次跑步即可解锁', 'medal-run-1', 'RUN_COUNT', 1, b'1'),
    (2, NOW(), NOW(), 'TEN_KM', '10 公里突破', '累计里程达到 10km', 'medal-run-10', 'TOTAL_DISTANCE', 10, b'1'),
    (3, NOW(), NOW(), 'POINTS_300', '活力冲榜者', '累计积分达到 300', 'medal-points-300', 'POINTS', 300, b'1')
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO level_rule (id, created_at, updated_at, level_number, title, min_points)
VALUES
    (1, NOW(), NOW(), 1, '起跑新人', 0),
    (2, NOW(), NOW(), 2, '坚持进阶', 120),
    (3, NOW(), NOW(), 3, '校园跑者', 240),
    (4, NOW(), NOW(), 4, '配速达人', 360),
    (5, NOW(), NOW(), 5, '燃脂王者', 520)
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO challenge_task (id, created_at, updated_at, title, description, task_type, goal_type, goal_value, points_reward, active)
VALUES
    (1, NOW(), NOW(), '今日起跑', '完成 1 次有效跑步', 'DAILY', 'RUN_COUNT', 1, 20, b'1'),
    (2, NOW(), NOW(), '本周 10 公里', '累计完成 10km 跑步', 'WEEKLY', 'DISTANCE_KM', 10, 60, b'1'),
    (3, NOW(), NOW(), '赛季耐力挑战', '累计跑步 180 分钟', 'SEASON', 'DURATION_MINUTES', 180, 120, b'1')
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO user_task_progress (id, created_at, updated_at, user_id, task_id, current_value, completed, completed_at)
VALUES
    (1, NOW(), NOW(), 2, 1, 1, b'1', NOW()),
    (2, NOW(), NOW(), 2, 2, 8, b'0', NULL),
    (3, NOW(), NOW(), 3, 1, 1, b'1', NOW()),
    (4, NOW(), NOW(), 3, 2, 10, b'1', NOW())
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO user_badge (id, created_at, updated_at, user_id, badge_id, granted_at)
VALUES
    (1, NOW(), NOW(), 2, 1, NOW()),
    (2, NOW(), NOW(), 2, 2, NOW()),
    (3, NOW(), NOW(), 3, 1, NOW()),
    (4, NOW(), NOW(), 3, 2, NOW()),
    (5, NOW(), NOW(), 3, 3, NOW())
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO club (id, created_at, updated_at, name, slogan, description, member_count, active)
VALUES
    (1, NOW(), NOW(), '晨曦跑团', '清晨 6 点见', '适合早起跑步爱好者的轻社群。', 42, b'1'),
    (2, NOW(), NOW(), '夜行者跑团', '夜跑不孤单', '周中夜跑训练和互相监督打卡。', 58, b'1')
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO club_message (id, created_at, updated_at, club_id, user_id, content)
VALUES
    (1, NOW(), NOW(), 1, 2, '明早 6:20 北门集合，目标 3km 轻松跑。'),
    (2, NOW(), NOW(), 2, 3, '今晚 8 点操场 10 圈，欢迎一起刷配速。')
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO feed_post (id, created_at, updated_at, user_id, content, image_urls, like_count, comment_count, report_count, risk_tags, featured, review_status)
VALUES
    (1, NOW(), NOW(), 2, '第一次把 5km 跑进 30 分钟，终于不是陪跑组了。', 'https://images.unsplash.com/photo-1541534401786-2077eed87a72', 12, 2, 0, NULL, b'1', 'APPROVED'),
    (2, NOW(), NOW(), 3, '环湖夜跑太舒服了，配速稳定在 5 分 40 秒。', 'https://images.unsplash.com/photo-1502904550040-7534597429ae', 9, 1, 0, NULL, b'0', 'APPROVED'),
    (3, NOW(), NOW(), 4, '求一个 5km 提速训练搭子。', NULL, 3, 0, 0, NULL, b'0', 'APPROVED')
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO post_comment (id, created_at, updated_at, post_id, user_id, content)
VALUES
    (1, NOW(), NOW(), 1, 3, '厉害，下一步冲 28 分。'),
    (2, NOW(), NOW(), 1, 4, '配速太稳了，向你学习。'),
    (3, NOW(), NOW(), 2, 2, '夜跑氛围确实比白天更好。')
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO post_like (id, created_at, updated_at, post_id, user_id)
VALUES
    (1, NOW(), NOW(), 1, 3),
    (2, NOW(), NOW(), 1, 4),
    (3, NOW(), NOW(), 2, 2)
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO activity (id, created_at, updated_at, title, location, start_time, end_time, registration_deadline, cover_url, description, max_capacity, status)
VALUES
    (1, NOW(), NOW(), '春季环湖夜跑赛', '学校南门集合', DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 5 DAY), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 4 DAY), 'https://images.unsplash.com/photo-1461896836934-ffe607ba8211', '面向全校学生的 5km 夜跑活动，配有打卡补给与摄影记录。', 200, 'PUBLISHED'),
    (2, NOW(), NOW(), '新生体测专项训练营', '东操场', DATE_ADD(NOW(), INTERVAL 8 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 8 DAY), INTERVAL 90 MINUTE), DATE_ADD(NOW(), INTERVAL 7 DAY), 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd', '针对 800/1000 米与耐力跑的专项训练。', 80, 'PUBLISHED')
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO activity_registration (id, created_at, updated_at, activity_id, user_id, status)
VALUES
    (1, NOW(), NOW(), 1, 2, 'REGISTERED'),
    (2, NOW(), NOW(), 1, 3, 'REGISTERED')
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO run_session (id, created_at, updated_at, user_id, state, started_at, paused_at, finished_at, duration_seconds, distance_km, avg_pace_seconds, calories, step_count, route_snapshot, source, route_point_count, device_model, device_platform, client_version)
VALUES
    (1, NOW(), NOW(), 2, 'FINISHED', DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 32 MINUTE), 1920, 5.2, 369, 312, 6500, '[]', 'wechat-miniapp', 0, 'dev-simulator', 'wechat', 'dev'),
    (2, NOW(), NOW(), 3, 'FINISHED', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 28 MINUTE), 1680, 5.0, 336, 300, 6100, '[]', 'wechat-miniapp', 0, 'dev-simulator', 'wechat', 'dev'),
    (3, NOW(), NOW(), 4, 'FINISHED', DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, DATE_ADD(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 35 MINUTE), 2100, 4.1, 512, 246, 5200, '[]', 'wechat-miniapp', 0, 'dev-simulator', 'wechat', 'dev')
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO audit_log (id, created_at, updated_at, operator_id, action, target_type, target_id, detail)
VALUES
    (1, NOW(), NOW(), 1, 'SEED_DATA', 'system', 'init', '初始化演示数据完成')
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);
