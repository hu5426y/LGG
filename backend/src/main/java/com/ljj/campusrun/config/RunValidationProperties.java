package com.ljj.campusrun.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "campusrun.run.validation")
public class RunValidationProperties {

    private double minDistanceKm = 0.5D;
    private int minDurationSeconds = 300;
    private double maxSpeedKmh = 25D;
    private int minRoutePoints = 5;
    private boolean allowSimulatedRuns;
}
