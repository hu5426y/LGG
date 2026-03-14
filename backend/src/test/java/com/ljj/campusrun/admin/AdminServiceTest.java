package com.ljj.campusrun.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ljj.campusrun.activity.ActivityService;
import com.ljj.campusrun.domain.entity.AuditLog;
import com.ljj.campusrun.domain.entity.User;
import com.ljj.campusrun.domain.enums.UserRole;
import com.ljj.campusrun.repository.AuditLogRepository;
import com.ljj.campusrun.repository.BadgeRepository;
import com.ljj.campusrun.repository.FeedPostRepository;
import com.ljj.campusrun.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminServiceTest {

    @Test
    void dashboardShouldAllowNullableAuditLogFields() {
        UserRepository userRepository = mock(UserRepository.class);
        FeedPostRepository feedPostRepository = mock(FeedPostRepository.class);
        BadgeRepository badgeRepository = mock(BadgeRepository.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        ActivityService activityService = mock(ActivityService.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminMetricsService adminMetricsService = mock(AdminMetricsService.class);
        AdminService adminService = new AdminService(
                userRepository,
                feedPostRepository,
                badgeRepository,
                auditLogRepository,
                activityService,
                auditLogService,
                passwordEncoder,
                adminMetricsService
        );

        User student = new User();
        student.setId(1L);
        student.setRole(UserRole.STUDENT);
        student.setDisplayName("测试同学");
        student.setTotalDistanceKm(8.2);
        student.setPoints(30);

        AuditLog log = new AuditLog();
        log.setId(99L);
        log.setAction("REVIEW_POST");
        log.setTargetType("feed_post");
        log.setTargetId(null);
        log.setDetail(null);
        log.setCreatedAt(LocalDateTime.of(2026, 3, 14, 12, 0));

        when(userRepository.findByRole(UserRole.STUDENT)).thenReturn(List.of(student));
        when(feedPostRepository.findByReviewStatusOrderByCreatedAtDesc(com.ljj.campusrun.domain.enums.ReviewStatus.PENDING))
                .thenReturn(List.of());
        when(auditLogRepository.findTop100ByOrderByCreatedAtDesc()).thenReturn(List.of(log));

        Map<String, Object> result = adminService.dashboard();

        assertEquals(1, result.get("studentCount"));
        assertEquals(8.2, result.get("totalDistanceKm"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> latestLogs = (List<Map<String, Object>>) result.get("latestLogs");
        assertEquals(1, latestLogs.size());
        assertNotNull(latestLogs.get(0));
        assertEquals("", latestLogs.get(0).get("targetId"));
        assertEquals("", latestLogs.get(0).get("detail"));
    }
}
