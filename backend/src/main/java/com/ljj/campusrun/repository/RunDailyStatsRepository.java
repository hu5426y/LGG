package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.RunDailyStats;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunDailyStatsRepository extends JpaRepository<RunDailyStats, Long> {

    Optional<RunDailyStats> findByStatDate(LocalDate statDate);

    List<RunDailyStats> findTop14ByOrderByStatDateDesc();
}
