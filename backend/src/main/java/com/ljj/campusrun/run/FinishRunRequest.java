package com.ljj.campusrun.run;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FinishRunRequest(
        @NotNull Double distanceKm,
        @NotNull Integer durationSeconds,
        @NotNull Integer stepCount,
        @Valid List<RunRoutePointRequest> routePoints
) {
    public FinishRunRequest {
        routePoints = routePoints == null ? List.of() : List.copyOf(routePoints);
    }
}
