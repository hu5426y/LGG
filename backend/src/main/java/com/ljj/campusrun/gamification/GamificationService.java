package com.ljj.campusrun.gamification;

import com.ljj.campusrun.domain.entity.Badge;
import com.ljj.campusrun.domain.entity.LevelRule;
import com.ljj.campusrun.domain.entity.RunSession;
import com.ljj.campusrun.domain.entity.User;
import com.ljj.campusrun.domain.entity.UserBadge;
import com.ljj.campusrun.domain.entity.UserTaskProgress;
import com.ljj.campusrun.domain.enums.RunState;
import com.ljj.campusrun.repository.BadgeRepository;
import com.ljj.campusrun.repository.ChallengeTaskRepository;
import com.ljj.campusrun.repository.ClubMemberRepository;
import com.ljj.campusrun.repository.DailyCheckinRepository;
import com.ljj.campusrun.repository.LevelRuleRepository;
import com.ljj.campusrun.repository.RunSessionRepository;
import com.ljj.campusrun.repository.UserBadgeRepository;
import com.ljj.campusrun.repository.UserTaskProgressRepository;
import com.ljj.campusrun.repository.UserRepository;
import com.ljj.campusrun.repository.UserRunPlanRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GamificationService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final ChallengeTaskRepository challengeTaskRepository;
    private final UserTaskProgressRepository userTaskProgressRepository;
    private final LevelRuleRepository levelRuleRepository;
    private final RunSessionRepository runSessionRepository;
    private final UserRepository userRepository;
    private final DailyCheckinRepository dailyCheckinRepository;
    private final UserRunPlanRepository userRunPlanRepository;
    private final ClubMemberRepository clubMemberRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getOverview(Long userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("badges", userBadgeRepository.findByUserIdOrderByGrantedAtDesc(userId).stream()
                .map(userBadge -> Map.<String, Object>of(
                        "id", userBadge.getBadge().getId(),
                        "name", userBadge.getBadge().getName(),
                        "description", userBadge.getBadge().getDescription() == null ? "" : userBadge.getBadge().getDescription(),
                        "icon", userBadge.getBadge().getIcon() == null ? "" : userBadge.getBadge().getIcon(),
                        "grantedAt", userBadge.getGrantedAt()
                ))
                .toList());
        data.put("tasks", userTaskProgressRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(progress -> {
                    Map<String, Object> task = new LinkedHashMap<>();
                    task.put("id", progress.getTask().getId());
                    task.put("title", progress.getTask().getTitle());
                    task.put("taskType", progress.getTask().getTaskType());
                    task.put("goalValue", progress.getTask().getGoalValue());
                    task.put("goalType", progress.getTask().getGoalType());
                    task.put("pointsReward", progress.getTask().getPointsReward());

                    Map<String, Object> taskProgress = new LinkedHashMap<>();
                    taskProgress.put("id", progress.getId());
                    taskProgress.put("currentValue", progress.getCurrentValue());
                    taskProgress.put("completed", progress.getCompleted());
                    taskProgress.put("completedAt", progress.getCompletedAt());
                    taskProgress.put("task", task);
                    return taskProgress;
                })
                .toList());
        data.put("levels", levelRuleRepository.findAllByOrderByMinPointsAsc());
        return data;
    }

    @Transactional
    public void applyRunRewards(User user, RunSession runSession) {
        int earnedPoints = Math.max(10, (int) Math.round(runSession.getDistanceKm() * 20));
        user.setPoints(user.getPoints() + earnedPoints);
        updateLevel(user);
        updateTaskProgress(user, runSession, earnedPoints);
        awardBadges(user);
        userRepository.save(user);
    }

    private void updateLevel(User user) {
        List<LevelRule> rules = levelRuleRepository.findAllByOrderByMinPointsAsc();
        int level = 1;
        for (LevelRule rule : rules) {
            if (user.getPoints() >= rule.getMinPoints()) {
                level = rule.getLevelNumber();
            }
        }
        user.setLevelValue(level);
    }

    private void updateTaskProgress(User user, RunSession runSession, int earnedPoints) {
        challengeTaskRepository.findByActiveTrueOrderByTaskTypeAscCreatedAtAsc().forEach(task -> {
            UserTaskProgress progress = userTaskProgressRepository.findByUserIdAndTaskId(user.getId(), task.getId())
                    .orElseGet(() -> {
                        UserTaskProgress created = new UserTaskProgress();
                        created.setUser(user);
                        created.setTask(task);
                        return created;
                    });
            int increment = switch (task.getGoalType()) {
                case "DISTANCE_KM" -> (int) Math.floor(runSession.getDistanceKm());
                case "RUN_COUNT" -> 1;
                case "DURATION_MINUTES" -> Math.max(1, runSession.getDurationSeconds() / 60);
                default -> 0;
            };
            progress.setCurrentValue(progress.getCurrentValue() + increment);
            if (!progress.getCompleted() && progress.getCurrentValue() >= task.getGoalValue()) {
                progress.setCompleted(true);
                progress.setCompletedAt(LocalDateTime.now());
                user.setPoints(user.getPoints() + task.getPointsReward());
            }
            userTaskProgressRepository.save(progress);
        });
        user.setPoints(user.getPoints() + earnedPoints);
    }

    private void awardBadges(User user) {
        long finishedRuns = runSessionRepository.countByUserIdAndState(user.getId(), RunState.FINISHED);
        int latestStreak = dailyCheckinRepository.findTopByUserIdAndCheckinDateLessThanOrderByCheckinDateDesc(
                        user.getId(), LocalDate.now().plusDays(1))
                .map(checkin -> checkin.getStreakDays())
                .orElse(0);
        long completedPlans = userRunPlanRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(plan -> "COMPLETED".equals(plan.getStatus()))
                .count();
        long squadParticipation = clubMemberRepository.countByUserIdAndActiveTrue(user.getId());
        List<Badge> badges = badgeRepository.findByActiveTrueOrderByRuleThresholdAsc();
        for (Badge badge : badges) {
            boolean shouldGrant = switch (badge.getRuleType()) {
                case "TOTAL_DISTANCE" -> user.getTotalDistanceKm() >= badge.getRuleThreshold();
                case "RUN_COUNT" -> finishedRuns >= badge.getRuleThreshold();
                case "POINTS" -> user.getPoints() >= badge.getRuleThreshold();
                case "CHECKIN_STREAK" -> latestStreak >= badge.getRuleThreshold();
                case "PLAN_COMPLETION" -> completedPlans >= badge.getRuleThreshold();
                case "SQUAD_PARTICIPATION" -> squadParticipation >= badge.getRuleThreshold();
                default -> false;
            };
            if (shouldGrant && !userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId())) {
                UserBadge userBadge = new UserBadge();
                userBadge.setUser(user);
                userBadge.setBadge(badge);
                userBadge.setGrantedAt(LocalDateTime.now());
                userBadgeRepository.save(userBadge);
            }
        }
    }
}
