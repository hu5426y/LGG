package com.ljj.campusrun.checkin;

import com.ljj.campusrun.domain.entity.DailyCheckin;
import com.ljj.campusrun.domain.entity.RunSession;
import com.ljj.campusrun.domain.entity.User;
import com.ljj.campusrun.domain.entity.UserRunPlanDay;
import com.ljj.campusrun.repository.DailyCheckinRepository;
import com.ljj.campusrun.repository.UserRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckinService {

    private final DailyCheckinRepository dailyCheckinRepository;
    private final UserRepository userRepository;

    @Transactional
    public DailyCheckin recordRunCheckin(Long userId, RunSession runSession, UserRunPlanDay planDay) {
        LocalDate today = LocalDate.now();
        DailyCheckin existing = dailyCheckinRepository.findByUserIdAndCheckinDate(userId, today).orElse(null);
        if (existing != null) {
            if (existing.getPlanDay() == null && planDay != null) {
                existing.setPlanDay(planDay);
                return dailyCheckinRepository.save(existing);
            }
            return existing;
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        DailyCheckin yesterday = dailyCheckinRepository.findTopByUserIdAndCheckinDateLessThanOrderByCheckinDateDesc(userId, today)
                .orElse(null);
        int streak = yesterday != null && yesterday.getCheckinDate().plusDays(1).equals(today)
                ? yesterday.getStreakDays() + 1
                : 1;
        DailyCheckin checkin = new DailyCheckin();
        checkin.setUser(user);
        checkin.setCheckinDate(today);
        checkin.setSourceRun(runSession);
        checkin.setPlanDay(planDay);
        checkin.setStreakDays(streak);
        return dailyCheckinRepository.save(checkin);
    }

    @Transactional(readOnly = true)
    public Object getToday(Long userId) {
        LocalDate today = LocalDate.now();
        DailyCheckin latest = dailyCheckinRepository.findByUserIdAndCheckinDate(userId, today).orElse(null);
        int streak = latest == null ? latestStreak(userId) : latest.getStreakDays();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("checkedIn", latest != null);
        data.put("checkinDate", today);
        data.put("streakDays", streak);
        data.put("sourceRunId", latest == null ? null : latest.getSourceRun().getId());
        data.put("planDayId", latest == null || latest.getPlanDay() == null ? null : latest.getPlanDay().getId());
        return data;
    }

    @Transactional(readOnly = true)
    public Object getHistory(Long userId) {
        return dailyCheckinRepository.findTop30ByUserIdOrderByCheckinDateDesc(userId).stream()
                .map(checkin -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("id", checkin.getId());
                    data.put("checkinDate", checkin.getCheckinDate());
                    data.put("streakDays", checkin.getStreakDays());
                    data.put("sourceRunId", checkin.getSourceRun().getId());
                    data.put("planDayId", checkin.getPlanDay() == null ? null : checkin.getPlanDay().getId());
                    return data;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public long countByDate(LocalDate date) {
        return dailyCheckinRepository.countByCheckinDate(date);
    }

    @Transactional(readOnly = true)
    public int latestStreak(Long userId) {
        return dailyCheckinRepository.findTopByUserIdAndCheckinDateLessThanOrderByCheckinDateDesc(userId, LocalDate.now().plusDays(1))
                .map(DailyCheckin::getStreakDays)
                .orElse(0);
    }
}
