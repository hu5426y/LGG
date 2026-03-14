package com.ljj.campusrun.runplan;

import jakarta.validation.constraints.NotNull;

public record SelectRunPlanRequest(@NotNull Long templateId) {
}
