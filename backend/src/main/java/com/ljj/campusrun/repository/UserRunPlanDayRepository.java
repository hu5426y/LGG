package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.UserRunPlanDay;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRunPlanDayRepository extends JpaRepository<UserRunPlanDay, Long> {

    List<UserRunPlanDay> findByUserRunPlanIdOrderByDayIndexAsc(Long userRunPlanId);

    Optional<UserRunPlanDay> findFirstByUserRunPlanIdAndCompletedFalseOrderByDayIndexAsc(Long userRunPlanId);

    long countByUserRunPlanIdAndCompletedTrue(Long userRunPlanId);
}
