package com.ljj.campusrun.activity;

import com.ljj.campusrun.domain.enums.ActivityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CreateActivityRequest(
        @NotBlank String title,
        String location,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        @NotNull LocalDateTime registrationDeadline,
        String coverUrl,
        @NotBlank String description,
        Integer maxCapacity,
        ActivityStatus status
) {
}
