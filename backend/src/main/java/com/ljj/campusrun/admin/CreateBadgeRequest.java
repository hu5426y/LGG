package com.ljj.campusrun.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBadgeRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        String icon,
        @NotBlank String ruleType,
        @NotNull Integer ruleThreshold
) {
}
