package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.Activity;
import com.ljj.campusrun.domain.enums.ActivityStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByStatusOrderByStartTimeAsc(ActivityStatus status);
}
