package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.Badge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    List<Badge> findByActiveTrueOrderByRuleThresholdAsc();
}
