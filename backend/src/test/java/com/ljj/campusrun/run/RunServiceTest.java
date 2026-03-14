package com.ljj.campusrun.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ljj.campusrun.admin.AuditLogService;
import com.ljj.campusrun.admin.AdminMetricsService;
import com.ljj.campusrun.checkin.CheckinService;
import com.ljj.campusrun.config.RunValidationProperties;
import com.ljj.campusrun.domain.entity.RunSession;
import com.ljj.campusrun.domain.entity.User;
import com.ljj.campusrun.domain.enums.RunState;
import com.ljj.campusrun.gamification.GamificationService;
import com.ljj.campusrun.gamification.RankingService;
import com.ljj.campusrun.repository.RunSessionRepository;
import com.ljj.campusrun.repository.UserRepository;
import com.ljj.campusrun.runplan.RunPlanService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RunServiceTest {

    @Test
    void startShouldReturnRunDataWhenFinishedAtIsNull() {
        RunSessionRepository runSessionRepository = mock(RunSessionRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        GamificationService gamificationService = mock(GamificationService.class);
        RankingService rankingService = mock(RankingService.class);
        RunValidationProperties runValidationProperties = new RunValidationProperties();
        ObjectMapper objectMapper = new ObjectMapper();
        AuditLogService auditLogService = mock(AuditLogService.class);
        RunPlanService runPlanService = mock(RunPlanService.class);
        CheckinService checkinService = mock(CheckinService.class);
        AdminMetricsService adminMetricsService = mock(AdminMetricsService.class);
        RunService runService = new RunService(
                runSessionRepository,
                userRepository,
                gamificationService,
                rankingService,
                runValidationProperties,
                objectMapper,
                auditLogService,
                runPlanService,
                checkinService,
                adminMetricsService
        );

        User user = new User();
        user.setId(1L);

        when(runSessionRepository.findFirstByUserIdAndStateInOrderByStartedAtDesc(1L, java.util.List.of(RunState.RUNNING, RunState.PAUSED)))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(runSessionRepository.save(any(RunSession.class))).thenAnswer(invocation -> {
            RunSession runSession = invocation.getArgument(0);
            runSession.setId(99L);
            return runSession;
        });

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) runService.start(
                1L,
                new StartRunRequest("wechat-miniapp", "iPhone", "ios", "8.0.0", "REAL", false)
        );

        assertEquals(99L, result.get("id"));
        assertEquals(RunState.RUNNING, result.get("state"));
        assertEquals("wechat-miniapp", result.get("source"));
        assertNull(result.get("finishedAt"));
    }

    @Test
    void discardShouldDeleteOwnedActiveRun() {
        RunSessionRepository runSessionRepository = mock(RunSessionRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        GamificationService gamificationService = mock(GamificationService.class);
        RankingService rankingService = mock(RankingService.class);
        RunValidationProperties runValidationProperties = new RunValidationProperties();
        ObjectMapper objectMapper = new ObjectMapper();
        AuditLogService auditLogService = mock(AuditLogService.class);
        RunPlanService runPlanService = mock(RunPlanService.class);
        CheckinService checkinService = mock(CheckinService.class);
        AdminMetricsService adminMetricsService = mock(AdminMetricsService.class);
        RunService runService = new RunService(
                runSessionRepository,
                userRepository,
                gamificationService,
                rankingService,
                runValidationProperties,
                objectMapper,
                auditLogService,
                runPlanService,
                checkinService,
                adminMetricsService
        );

        User user = new User();
        user.setId(2L);

        RunSession runSession = new RunSession();
        runSession.setId(4L);
        runSession.setUser(user);
        runSession.setState(RunState.PAUSED);

        when(runSessionRepository.findByIdAndUserId(4L, 2L)).thenReturn(Optional.of(runSession));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) runService.discard(2L, 4L);

        assertEquals(4L, result.get("id"));
        assertTrue((Boolean) result.get("discarded"));
        verify(runSessionRepository).delete(runSession);
    }
}
