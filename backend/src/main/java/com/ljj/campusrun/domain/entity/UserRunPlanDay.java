package com.ljj.campusrun.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_run_plan_day")
public class UserRunPlanDay extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_run_plan_id")
    private UserRunPlan userRunPlan;

    @Column(nullable = false)
    private Integer dayIndex;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Double targetDistanceKm = 0D;

    @Column(nullable = false)
    private Integer targetDurationMinutes = 0;

    @Column(nullable = false)
    private Boolean completed = false;

    private LocalDate completedOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_run_id")
    private RunSession sourceRun;
}
