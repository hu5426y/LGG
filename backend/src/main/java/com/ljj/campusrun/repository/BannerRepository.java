package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.Banner;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findByActiveTrueOrderBySortOrderAsc();
}
