package com.ljj.campusrun.checkin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ljj.campusrun.domain.entity.DailyCheckin;
import com.ljj.campusrun.domain.entity.RunSession;
import com.ljj.campusrun.domain.entity.User;
import com.ljj.campusrun.repository.DailyCheckinRepository;
import com.ljj.campusrun.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CheckinServiceTest {

    @Test
    void recordRunCheckinShouldIncreaseStreakWhenYesterdayExists() {
        DailyCheckinRepository dailyCheckinRepository = mock(DailyCheckinRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CheckinService checkinService = new CheckinService(dailyCheckinRepository, userRepository);

        User user = new User();
        user.setId(1L);

        RunSession runSession = new RunSession();
        runSession.setId(11L);

        DailyCheckin yesterday = new DailyCheckin();
        yesterday.setCheckinDate(LocalDate.now().minusDays(1));
        yesterday.setStreakDays(2);

        when(dailyCheckinRepository.findByUserIdAndCheckinDate(1L, LocalDate.now())).thenReturn(Optional.empty());
        when(dailyCheckinRepository.findTopByUserIdAndCheckinDateLessThanOrderByCheckinDateDesc(1L, LocalDate.now()))
                .thenReturn(Optional.of(yesterday));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(dailyCheckinRepository.save(org.mockito.ArgumentMatchers.any(DailyCheckin.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DailyCheckin saved = checkinService.recordRunCheckin(1L, runSession, null);

        assertEquals(3, saved.getStreakDays());
        assertEquals(runSession, saved.getSourceRun());
    }
}
