package com.ljj.campusrun.domain.entity;

import com.ljj.campusrun.domain.enums.RunState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "run_session")
public class RunSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RunState state;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime pausedAt;

    private LocalDateTime finishedAt;

    @Column(nullable = false)
    private Integer durationSeconds = 0;

    @Column(nullable = false)
    private Double distanceKm = 0D;

    @Column(nullable = false)
    private Integer avgPaceSeconds = 0;

    @Column(nullable = false)
    private Integer calories = 0;

    @Column(nullable = false)
    private Integer stepCount = 0;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String routeSnapshot;

    @Column(length = 32)
    private String source;

    @Column(nullable = false)
    private Integer routePointCount = 0;

    @Column(length = 64)
    private String deviceModel;

    @Column(length = 32)
    private String devicePlatform;

    @Column(length = 32)
    private String clientVersion;
}
