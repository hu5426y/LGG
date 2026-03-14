package com.ljj.campusrun.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ljj.campusrun.domain.entity.RunSession;
import com.ljj.campusrun.domain.entity.User;
import com.ljj.campusrun.domain.enums.RunState;
import com.ljj.campusrun.repository.RunSessionRepository;
import com.ljj.campusrun.repository.UserBadgeRepository;
import com.ljj.campusrun.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserServiceTest {

    @Test
    void getCurrentUserProfileShouldAllowRecentRunFinishedAtToBeNull() {
        UserRepository userRepository = mock(UserRepository.class);
        UserBadgeRepository userBadgeRepository = mock(UserBadgeRepository.class);
        RunSessionRepository runSessionRepository = mock(RunSessionRepository.class);
        UserService userService = new UserService(userRepository, userBadgeRepository, runSessionRepository);

        User user = new User();
        user.setId(1L);
        user.setUsername("20230001");
        user.setDisplayName("测试同学");
        user.setPoints(20);
        user.setLevelValue(2);
        user.setTotalDistanceKm(6.5);
        user.setTotalDurationSeconds(1800);

        RunSession runSession = new RunSession();
        runSession.setId(10L);
        runSession.setState(RunState.RUNNING);
        runSession.setDistanceKm(2.5);
        runSession.setDurationSeconds(600);
        runSession.setAvgPaceSeconds(240);
        runSession.setCalories(150);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userBadgeRepository.findByUserIdOrderByGrantedAtDesc(1L)).thenReturn(List.of());
        when(runSessionRepository.countByUserIdAndState(1L, RunState.FINISHED)).thenReturn(0L);
        when(runSessionRepository.findTop20ByUserIdAndStateOrderByStartedAtDesc(1L, RunState.FINISHED)).thenReturn(List.of(runSession));

        Map<String, Object> result = userService.getCurrentUserProfile(1L);

        assertNotNull(result.get("profile"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recentRuns = (List<Map<String, Object>>) result.get("recentRuns");
        assertEquals(1, recentRuns.size());
        assertEquals(10L, recentRuns.get(0).get("id"));
        assertNull(recentRuns.get(0).get("finishedAt"));
    }
}
