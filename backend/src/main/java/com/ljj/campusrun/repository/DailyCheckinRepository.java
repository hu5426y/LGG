package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.DailyCheckin;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyCheckinRepository extends JpaRepository<DailyCheckin, Long> {

    Optional<DailyCheckin> findByUserIdAndCheckinDate(Long userId, LocalDate checkinDate);

    Optional<DailyCheckin> findTopByUserIdAndCheckinDateLessThanOrderByCheckinDateDesc(Long userId, LocalDate checkinDate);

    List<DailyCheckin> findTop30ByUserIdOrderByCheckinDateDesc(Long userId);

    long countByCheckinDate(LocalDate checkinDate);
}
