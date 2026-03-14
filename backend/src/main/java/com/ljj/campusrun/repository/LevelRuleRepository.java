package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.LevelRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LevelRuleRepository extends JpaRepository<LevelRule, Long> {

    List<LevelRule> findAllByOrderByMinPointsAsc();
}
