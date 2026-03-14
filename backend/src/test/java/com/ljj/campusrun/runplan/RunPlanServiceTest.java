package com.ljj.campusrun.runplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ljj.campusrun.domain.entity.RunPlanTemplate;
import com.ljj.campusrun.domain.entity.RunSession;
import com.ljj.campusrun.domain.entity.UserRunPlan;
import com.ljj.campusrun.domain.entity.UserRunPlanDay;
import com.ljj.campusrun.repository.RunPlanTemplateRepository;
import com.ljj.campusrun.repository.UserRepository;
import com.ljj.campusrun.repository.UserRunPlanDayRepository;
import com.ljj.campusrun.repository.UserRunPlanRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RunPlanServiceTest {

    @Test
    void applyRunShouldCompletePendingPlanDay() {
        RunPlanTemplateRepository templateRepository = mock(RunPlanTemplateRepository.class);
        UserRunPlanRepository userRunPlanRepository = mock(UserRunPlanRepository.class);
        UserRunPlanDayRepository userRunPlanDayRepository = mock(UserRunPlanDayRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        RunPlanService runPlanService = new RunPlanService(templateRepository, userRunPlanRepository, userRunPlanDayRepository, userRepository);

        RunPlanTemplate template = new RunPlanTemplate();
        template.setId(1L);
        template.setTitle("5km Beginner");

        UserRunPlan plan = new UserRunPlan();
        plan.setId(5L);
        plan.setTemplate(template);
        plan.setStatus(RunPlanService.STATUS_ACTIVE);
        plan.setStartedOn(LocalDate.now());
        plan.setCurrentDayIndex(1);

        UserRunPlanDay pendingDay = new UserRunPlanDay();
        pendingDay.setId(9L);
        pendingDay.setDayIndex(1);
        pendingDay.setTargetDistanceKm(1.5D);
        pendingDay.setTargetDurationMinutes(20);

        RunSession runSession = new RunSession();
        runSession.setDistanceKm(2.0D);
        runSession.setDurationSeconds(1500);

        when(userRunPlanRepository.findByUserIdAndStatus(1L, RunPlanService.STATUS_ACTIVE)).thenReturn(Optional.of(plan));
        when(userRunPlanDayRepository.findFirstByUserRunPlanIdAndCompletedFalseOrderByDayIndexAsc(5L))
                .thenReturn(Optional.of(pendingDay))
                .thenReturn(Optional.empty());
        when(userRunPlanDayRepository.save(org.mockito.ArgumentMatchers.any(UserRunPlanDay.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRunPlanRepository.save(org.mockito.ArgumentMatchers.any(UserRunPlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RunPlanService.PlanProgress progress = runPlanService.applyRun(1L, runSession);

        assertNotNull(progress.completedDay());
        assertEquals(true, progress.completedDay().getCompleted());
        assertEquals(RunPlanService.STATUS_COMPLETED, progress.plan().getStatus());
    }
}
