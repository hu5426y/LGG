package com.ljj.campusrun.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "run_plan_template")
public class RunPlanTemplate extends BaseEntity {

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 32)
    private String planType;

    @Column(nullable = false)
    private Integer durationDays;

    @Column(nullable = false)
    private Double targetDistanceKm = 0D;

    @Column(nullable = false)
    private Integer targetRuns = 0;

    @Column(nullable = false)
    private Boolean active = true;
}
