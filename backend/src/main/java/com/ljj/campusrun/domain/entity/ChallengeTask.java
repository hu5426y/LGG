package com.ljj.campusrun.domain.entity;

import com.ljj.campusrun.domain.enums.TaskType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "challenge_task")
public class ChallengeTask extends BaseEntity {

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TaskType taskType;

    @Column(nullable = false, length = 32)
    private String goalType;

    @Column(nullable = false)
    private Integer goalValue;

    @Column(nullable = false)
    private Integer pointsReward = 0;

    @Column(nullable = false)
    private Boolean active = true;
}
