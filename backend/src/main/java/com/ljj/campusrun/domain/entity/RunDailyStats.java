package com.ljj.campusrun.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "run_daily_stats")
public class RunDailyStats extends BaseEntity {

    @Column(nullable = false, unique = true)
    private LocalDate statDate;

    @Column(nullable = false)
    private Integer activeUsers = 0;

    @Column(nullable = false)
    private Double totalDistanceKm = 0D;

    @Column(nullable = false)
    private Integer totalDurationSeconds = 0;

    @Column(nullable = false)
    private Integer averagePaceSeconds = 0;

    @Column(nullable = false)
    private Integer completedPlans = 0;

    @Column(nullable = false)
    private Integer checkinUsers = 0;

    @Column(nullable = false)
    private Integer activeSquads = 0;

    @Column(nullable = false)
    private Integer squadMessageCount = 0;
}
