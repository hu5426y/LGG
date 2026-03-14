package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.UserTaskProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTaskProgressRepository extends JpaRepository<UserTaskProgress, Long> {

    List<UserTaskProgress> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserTaskProgress> findByUserIdAndTaskId(Long userId, Long taskId);
}
