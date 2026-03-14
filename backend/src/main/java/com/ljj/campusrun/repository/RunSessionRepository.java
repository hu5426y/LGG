package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.RunSession;
import com.ljj.campusrun.domain.enums.RunState;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunSessionRepository extends JpaRepository<RunSession, Long> {

    Optional<RunSession> findFirstByUserIdAndStateInOrderByStartedAtDesc(Long userId, List<RunState> states);

    List<RunSession> findTop20ByUserIdAndStateOrderByStartedAtDesc(Long userId, RunState state);

    List<RunSession> findTop100ByStateOrderByDistanceKmDesc(RunState state);

    long countByUserIdAndState(Long userId, RunState state);

    Optional<RunSession> findByIdAndUserId(Long runId, Long userId);
}
