package com.ljj.campusrun.run;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ljj.campusrun.admin.AuditLogService;
import com.ljj.campusrun.config.RunValidationProperties;
import com.ljj.campusrun.domain.entity.RunSession;
import com.ljj.campusrun.domain.entity.User;
import com.ljj.campusrun.domain.enums.RunState;
import com.ljj.campusrun.gamification.GamificationService;
import com.ljj.campusrun.gamification.RankingService;
import com.ljj.campusrun.repository.RunSessionRepository;
import com.ljj.campusrun.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RunValidationTest {

    @Test
    void finishShouldRejectWhenRoutePointsTooFew() {
        RunSessionRepository runSessionRepository = mock(RunSessionRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        GamificationService gamificationService = mock(GamificationService.class);
        RankingService rankingService = mock(RankingService.class);
        RunValidationProperties properties = new RunValidationProperties();
        ObjectMapper objectMapper = new ObjectMapper();
        AuditLogService auditLogService = mock(AuditLogService.class);
        RunService runService = new RunService(
                runSessionRepository,
                userRepository,
                gamificationService,
                rankingService,
                properties,
                objectMapper,
                auditLogService
        );

        User user = new User();
        user.setId(1L);

        RunSession runSession = new RunSession();
        runSession.setId(11L);
        runSession.setUser(user);
        runSession.setState(RunState.RUNNING);
        runSession.setStartedAt(LocalDateTime.now().minusMinutes(10));
        runSession.setSource("wechat-miniapp");

        when(runSessionRepository.findByIdAndUserId(11L, 1L)).thenReturn(Optional.of(runSession));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(runSessionRepository.save(any(RunSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinishRunRequest request = new FinishRunRequest(
                2.0,
                600,
                3000,
                List.of(
                        new RunRoutePointRequest(31.1, 121.1, 1000L, 5.0, 1.0),
                        new RunRoutePointRequest(31.1001, 121.1001, 5000L, 5.0, 1.2)
                )
        );

        assertThrows(IllegalArgumentException.class, () -> runService.finish(1L, 11L, request));
    }

    @Test
    void finishShouldTreatNullRoutePointsAsEmptyList() {
        RunSessionRepository runSessionRepository = mock(RunSessionRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        GamificationService gamificationService = mock(GamificationService.class);
        RankingService rankingService = mock(RankingService.class);
        RunValidationProperties properties = new RunValidationProperties();
        ObjectMapper objectMapper = new ObjectMapper();
        AuditLogService auditLogService = mock(AuditLogService.class);
        RunService runService = new RunService(
                runSessionRepository,
                userRepository,
                gamificationService,
                rankingService,
                properties,
                objectMapper,
                auditLogService
        );

        User user = new User();
        user.setId(1L);

        RunSession runSession = new RunSession();
        runSession.setId(12L);
        runSession.setUser(user);
        runSession.setState(RunState.RUNNING);
        runSession.setStartedAt(LocalDateTime.now().minusMinutes(10));
        runSession.setSource("wechat-miniapp");

        when(runSessionRepository.findByIdAndUserId(12L, 1L)).thenReturn(Optional.of(runSession));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                runService.finish(1L, 12L, new FinishRunRequest(2.0, 600, 3000, null)));

        assertEquals("轨迹点过少，请开启定位后重试", exception.getMessage());
    }
}
