package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.UserRunPlan;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRunPlanRepository extends JpaRepository<UserRunPlan, Long> {

    Optional<UserRunPlan> findByUserIdAndStatus(Long userId, String status);

    Optional<UserRunPlan> findByIdAndUserId(Long id, Long userId);

    long countByCompletedOn(LocalDate completedOn);

    List<UserRunPlan> findByUserIdOrderByCreatedAtDesc(Long userId);
}
