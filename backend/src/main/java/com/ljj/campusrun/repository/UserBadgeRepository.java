package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.UserBadge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    List<UserBadge> findByUserIdOrderByGrantedAtDesc(Long userId);

    boolean existsByUserIdAndBadgeId(Long userId, Long badgeId);
}
