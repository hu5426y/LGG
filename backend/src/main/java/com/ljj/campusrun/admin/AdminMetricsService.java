package com.ljj.campusrun.admin;

import com.ljj.campusrun.domain.entity.DailyCheckin;
import com.ljj.campusrun.domain.entity.RunDailyStats;
import com.ljj.campusrun.domain.enums.RunState;
import com.ljj.campusrun.domain.enums.UserRole;
import com.ljj.campusrun.repository.ClubMemberRepository;
import com.ljj.campusrun.repository.ClubMessageRepository;
import com.ljj.campusrun.repository.DailyCheckinRepository;
import com.ljj.campusrun.repository.RunDailyStatsRepository;
import com.ljj.campusrun.repository.RunSessionRepository;
import com.ljj.campusrun.repository.UserRepository;
import com.ljj.campusrun.repository.UserRunPlanRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminMetricsService {

    private final RunDailyStatsRepository runDailyStatsRepository;
    private final RunSessionRepository runSessionRepository;
    private final DailyCheckinRepository dailyCheckinRepository;
    private final UserRunPlanRepository userRunPlanRepository;
    private final ClubMessageRepository clubMessageRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public void refreshDailyStats(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);
        var runs = runSessionRepository.findByStateAndFinishedAtBetween(RunState.FINISHED, start, end);
        RunDailyStats stats = runDailyStatsRepository.findByStatDate(date).orElseGet(RunDailyStats::new);
        stats.setStatDate(date);
        stats.setActiveUsers((int) runSessionRepository.countDistinctUserIdByStateAndFinishedAtBetween(RunState.FINISHED, start, end));
        stats.setTotalDistanceKm(runs.stream().mapToDouble(run -> run.getDistanceKm() == null ? 0D : run.getDistanceKm()).sum());
        stats.setTotalDurationSeconds(runs.stream().mapToInt(run -> run.getDurationSeconds() == null ? 0 : run.getDurationSeconds()).sum());
        stats.setAveragePaceSeconds(runs.isEmpty()
                ? 0
                : (int) Math.round(runs.stream().mapToInt(run -> run.getAvgPaceSeconds() == null ? 0 : run.getAvgPaceSeconds()).average().orElse(0D)));
        stats.setCheckinUsers((int) dailyCheckinRepository.countByCheckinDate(date));
        stats.setCompletedPlans((int) userRunPlanRepository.countByCompletedOn(date));
        stats.setActiveSquads((int) clubMessageRepository.countDistinctClubIdByCreatedAtBetween(start, end));
        stats.setSquadMessageCount((int) clubMessageRepository.countByCreatedAtBetween(start, end));
        runDailyStatsRepository.save(stats);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOverview() {
        LocalDate today = LocalDate.now();
        RunDailyStats todayStats = runDailyStatsRepository.findByStatDate(today).orElse(null);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("today", todayStats == null
                ? Map.of("activeUsers", 0, "totalDistanceKm", 0D, "averagePaceSeconds", 0, "checkinUsers", 0)
                : mapDailyStats(todayStats));
        data.put("totals", Map.of(
                "students", userRepository.findByRole(UserRole.STUDENT).size(),
                "joinedSquadMembers", clubMemberRepository.count()
        ));
        data.put("streakBuckets", List.of(
                bucket("1-2天", countStreaks(1, 2)),
                bucket("3-6天", countStreaks(3, 6)),
                bucket("7天+", countStreaks(7, Integer.MAX_VALUE))
        ));
        return data;
    }

    @Transactional(readOnly = true)
    public Object getTrends() {
        return runDailyStatsRepository.findTop14ByOrderByStatDateDesc().stream()
                .sorted((left, right) -> left.getStatDate().compareTo(right.getStatDate()))
                .map(this::mapDailyStats)
                .toList();
    }

    private Map<String, Object> bucket(String label, long value) {
        return Map.of("label", label, "value", value);
    }

    private long countStreaks(int min, int max) {
        return dailyCheckinRepository.findAll().stream()
                .collect(Collectors.toMap(
                        checkin -> checkin.getUser().getId(),
                        checkin -> checkin,
                        (left, right) -> left.getCheckinDate().isAfter(right.getCheckinDate()) ? left : right
                ))
                .values()
                .stream()
                .map(DailyCheckin::getStreakDays)
                .filter(streak -> streak >= min && streak <= max)
                .count();
    }

    private Map<String, Object> mapDailyStats(RunDailyStats stats) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("date", stats.getStatDate());
        data.put("activeUsers", stats.getActiveUsers());
        data.put("totalDistanceKm", stats.getTotalDistanceKm());
        data.put("totalDurationSeconds", stats.getTotalDurationSeconds());
        data.put("averagePaceSeconds", stats.getAveragePaceSeconds());
        data.put("completedPlans", stats.getCompletedPlans());
        data.put("checkinUsers", stats.getCheckinUsers());
        data.put("activeSquads", stats.getActiveSquads());
        data.put("squadMessageCount", stats.getSquadMessageCount());
        return data;
    }
}
