package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.RunSession;
import com.ljj.campusrun.domain.enums.RunState;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RunSessionRepository extends JpaRepository<RunSession, Long> {

    Optional<RunSession> findFirstByUserIdAndStateInOrderByStartedAtDesc(Long userId, List<RunState> states);

    List<RunSession> findTop20ByUserIdAndStateOrderByStartedAtDesc(Long userId, RunState state);

    List<RunSession> findTop100ByStateOrderByDistanceKmDesc(RunState state);

    long countByUserIdAndState(Long userId, RunState state);

    Optional<RunSession> findByIdAndUserId(Long runId, Long userId);

    List<RunSession> findByStateAndFinishedAtBetween(RunState state, LocalDateTime start, LocalDateTime end);

    @Query("select count(distinct r.user.id) from RunSession r where r.state = :state and r.finishedAt between :start and :end")
    long countDistinctUserIdByStateAndFinishedAtBetween(RunState state, LocalDateTime start, LocalDateTime end);
}
