package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.Club;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRepository extends JpaRepository<Club, Long> {

    List<Club> findByActiveTrueOrderByMemberCountDesc();
}
