package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.ClubMessage;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClubMessageRepository extends JpaRepository<ClubMessage, Long> {

    List<ClubMessage> findTop30ByClubIdOrderByCreatedAtDesc(Long clubId);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("select count(distinct m.club.id) from ClubMessage m where m.createdAt between :start and :end")
    long countDistinctClubIdByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
