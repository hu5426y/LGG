package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.ClubMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {

    boolean existsByClubIdAndUserIdAndActiveTrue(Long clubId, Long userId);

    Optional<ClubMember> findByClubIdAndUserId(Long clubId, Long userId);

    List<ClubMember> findByClubIdAndActiveTrueOrderByJoinedAtAsc(Long clubId);

    long countByUserIdAndActiveTrue(Long userId);
}
