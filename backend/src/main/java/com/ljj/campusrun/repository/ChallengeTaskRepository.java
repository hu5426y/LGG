package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.ChallengeTask;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeTaskRepository extends JpaRepository<ChallengeTask, Long> {

    List<ChallengeTask> findByActiveTrueOrderByTaskTypeAscCreatedAtAsc();
}
