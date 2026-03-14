package com.ljj.campusrun.run;

import jakarta.validation.constraints.NotNull;

public record RunRoutePointRequest(
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotNull Long timestamp,
        Double accuracy,
        Double speedMetersPerSecond
) {
}
