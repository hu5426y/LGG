package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.ClubMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubMessageRepository extends JpaRepository<ClubMessage, Long> {

    List<ClubMessage> findTop30ByClubIdOrderByCreatedAtDesc(Long clubId);
}
