package com.ljj.campusrun.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "badge")
public class Badge extends BaseEntity {

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(length = 255)
    private String icon;

    @Column(nullable = false, length = 32)
    private String ruleType;

    @Column(nullable = false)
    private Integer ruleThreshold;

    @Column(nullable = false)
    private Boolean active = true;
}
