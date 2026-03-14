package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.Tutorial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TutorialRepository extends JpaRepository<Tutorial, Long> {

    List<Tutorial> findTop5ByPublishedTrueOrderByCreatedAtDesc();
}
