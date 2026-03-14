package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.RunPlanTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunPlanTemplateRepository extends JpaRepository<RunPlanTemplate, Long> {

    List<RunPlanTemplate> findByActiveTrueOrderByIdAsc();

    Optional<RunPlanTemplate> findByIdAndActiveTrue(Long id);
}
