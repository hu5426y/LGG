package com.ljj.campusrun.domain.entity;

import com.ljj.campusrun.domain.enums.ActivityStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "activity")
public class Activity extends BaseEntity {

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 128)
    private String location;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private LocalDateTime registrationDeadline;

    @Column(length = 255)
    private String coverUrl;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String description;

    @Column(nullable = false)
    private Integer maxCapacity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ActivityStatus status;
}
