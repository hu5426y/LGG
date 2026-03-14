package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.ActivityRegistration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRegistrationRepository extends JpaRepository<ActivityRegistration, Long> {

    long countByActivityIdAndStatus(Long activityId, com.ljj.campusrun.domain.enums.RegistrationStatus status);

    Optional<ActivityRegistration> findByActivityIdAndUserId(Long activityId, Long userId);
}
