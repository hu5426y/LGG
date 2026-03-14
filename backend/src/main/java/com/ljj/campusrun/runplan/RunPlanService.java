package com.ljj.campusrun.runplan;

import com.ljj.campusrun.domain.entity.RunPlanTemplate;
import com.ljj.campusrun.domain.entity.RunSession;
import com.ljj.campusrun.domain.entity.User;
import com.ljj.campusrun.domain.entity.UserRunPlan;
import com.ljj.campusrun.domain.entity.UserRunPlanDay;
import com.ljj.campusrun.repository.RunPlanTemplateRepository;
import com.ljj.campusrun.repository.UserRepository;
import com.ljj.campusrun.repository.UserRunPlanDayRepository;
import com.ljj.campusrun.repository.UserRunPlanRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RunPlanService {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final RunPlanTemplateRepository runPlanTemplateRepository;
    private final UserRunPlanRepository userRunPlanRepository;
    private final UserRunPlanDayRepository userRunPlanDayRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Object listTemplates() {
        return runPlanTemplateRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(this::mapTemplate)
                .toList();
    }

    @Transactional(readOnly = true)
    public Object getCurrentPlan(Long userId) {
        return userRunPlanRepository.findByUserIdAndStatus(userId, STATUS_ACTIVE)
                .map(this::mapPlanDetail)
                .orElse(Map.of(
                        "active", false,
                        "days", List.of()
                ));
    }

    @Transactional
    public Object selectPlan(Long userId, Long templateId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        RunPlanTemplate template = runPlanTemplateRepository.findByIdAndActiveTrue(templateId)
                .orElseThrow(() -> new IllegalArgumentException("计划模板不存在"));
        userRunPlanRepository.findByUserIdAndStatus(userId, STATUS_ACTIVE).ifPresent(existing -> {
            existing.setStatus(STATUS_CANCELLED);
            userRunPlanRepository.save(existing);
        });

        UserRunPlan plan = new UserRunPlan();
        plan.setUser(user);
        plan.setTemplate(template);
        plan.setStatus(STATUS_ACTIVE);
        plan.setStartedOn(LocalDate.now());
        plan.setCurrentDayIndex(1);
        UserRunPlan savedPlan = userRunPlanRepository.save(plan);
        for (UserRunPlanDay day : buildPlanDays(savedPlan)) {
            userRunPlanDayRepository.save(day);
        }
        return mapPlanDetail(savedPlan);
    }

    @Transactional
    public PlanProgress applyRun(Long userId, RunSession runSession) {
        return userRunPlanRepository.findByUserIdAndStatus(userId, STATUS_ACTIVE)
                .map(plan -> advancePlan(plan, runSession))
                .orElseGet(() -> PlanProgress.empty(null));
    }

    @Transactional(readOnly = true)
    public long countCompletedPlans(LocalDate date) {
        return userRunPlanRepository.countByCompletedOn(date);
    }

    private PlanProgress advancePlan(UserRunPlan plan, RunSession runSession) {
        UserRunPlanDay day = userRunPlanDayRepository.findFirstByUserRunPlanIdAndCompletedFalseOrderByDayIndexAsc(plan.getId())
                .orElse(null);
        if (day == null) {
            if (!STATUS_COMPLETED.equals(plan.getStatus())) {
                plan.setStatus(STATUS_COMPLETED);
                plan.setCompletedOn(LocalDate.now());
                userRunPlanRepository.save(plan);
            }
            return PlanProgress.empty(plan);
        }
        boolean distanceReached = runSession.getDistanceKm() >= day.getTargetDistanceKm();
        boolean durationReached = runSession.getDurationSeconds() >= day.getTargetDurationMinutes() * 60;
        if (!distanceReached && !durationReached) {
            return PlanProgress.empty(plan);
        }
        day.setCompleted(true);
        day.setCompletedOn(LocalDate.now());
        day.setSourceRun(runSession);
        userRunPlanDayRepository.save(day);
        plan.setCurrentDayIndex(day.getDayIndex() + 1);
        if (userRunPlanDayRepository.findFirstByUserRunPlanIdAndCompletedFalseOrderByDayIndexAsc(plan.getId()).isEmpty()) {
            plan.setStatus(STATUS_COMPLETED);
            plan.setCompletedOn(LocalDate.now());
        }
        userRunPlanRepository.save(plan);
        return new PlanProgress(plan, day);
    }

    private List<UserRunPlanDay> buildPlanDays(UserRunPlan plan) {
        List<UserRunPlanDay> days = new ArrayList<>();
        RunPlanTemplate template = plan.getTemplate();
        for (int i = 1; i <= template.getDurationDays(); i++) {
            UserRunPlanDay day = new UserRunPlanDay();
            day.setUserRunPlan(plan);
            day.setDayIndex(i);
            day.setTitle("Day " + i + " 训练");
            if ("BEGINNER_5K".equals(template.getCode())) {
                day.setDescription(i % 3 == 0 ? "恢复与轻松跑结合，保持节奏。" : "轻松慢跑，逐步延长距离。");
                day.setTargetDistanceKm(Math.min(template.getTargetDistanceKm(), 1.2D + (i * 0.25D)));
                day.setTargetDurationMinutes(18 + i * 2);
            } else if ("FAT_BURN".equals(template.getCode())) {
                day.setDescription("控制心率，维持稳定有氧跑。");
                day.setTargetDistanceKm(2.5D + ((i + 1) % 2) * 0.8D);
                day.setTargetDurationMinutes(24 + i);
            } else {
                day.setDescription("保持周目标节奏，完成每日有效打卡。");
                day.setTargetDistanceKm(template.getTargetDistanceKm());
                day.setTargetDurationMinutes(20 + i);
            }
            days.add(day);
        }
        return days;
    }

    private Map<String, Object> mapTemplate(RunPlanTemplate template) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", template.getId());
        data.put("code", template.getCode());
        data.put("title", template.getTitle());
        data.put("description", template.getDescription() == null ? "" : template.getDescription());
        data.put("planType", template.getPlanType());
        data.put("durationDays", template.getDurationDays());
        data.put("targetDistanceKm", template.getTargetDistanceKm());
        data.put("targetRuns", template.getTargetRuns());
        return data;
    }

    private Map<String, Object> mapPlanDetail(UserRunPlan plan) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("active", STATUS_ACTIVE.equals(plan.getStatus()));
        data.put("status", plan.getStatus());
        data.put("startedOn", plan.getStartedOn());
        data.put("completedOn", plan.getCompletedOn());
        data.put("currentDayIndex", plan.getCurrentDayIndex());
        data.put("template", mapTemplate(plan.getTemplate()));
        data.put("days", userRunPlanDayRepository.findByUserRunPlanIdOrderByDayIndexAsc(plan.getId()).stream()
                .map(day -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", day.getId());
                    item.put("dayIndex", day.getDayIndex());
                    item.put("title", day.getTitle());
                    item.put("description", day.getDescription() == null ? "" : day.getDescription());
                    item.put("targetDistanceKm", day.getTargetDistanceKm());
                    item.put("targetDurationMinutes", day.getTargetDurationMinutes());
                    item.put("completed", day.getCompleted());
                    item.put("completedOn", day.getCompletedOn());
                    item.put("sourceRunId", day.getSourceRun() == null ? null : day.getSourceRun().getId());
                    return item;
                })
                .toList());
        return data;
    }

    public record PlanProgress(UserRunPlan plan, UserRunPlanDay completedDay) {
        static PlanProgress empty(UserRunPlan plan) {
            return new PlanProgress(plan, null);
        }
    }
}
