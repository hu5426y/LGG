package com.ljj.campusrun.run;

public record StartRunRequest(
        String source,
        String deviceModel,
        String devicePlatform,
        String clientVersion,
        String mode,
        Boolean simulated
) {
}
