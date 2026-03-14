package com.ljj.campusrun.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "level_rule")
public class LevelRule extends BaseEntity {

    @Column(nullable = false)
    private Integer levelNumber;

    @Column(nullable = false, length = 64)
    private String title;

    @Column(nullable = false)
    private Integer minPoints;
}
