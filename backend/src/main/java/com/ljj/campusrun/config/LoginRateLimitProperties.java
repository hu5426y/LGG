package com.ljj.campusrun.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "campusrun.login.rate-limit")
public class LoginRateLimitProperties {

    private int maxAttempts = 12;
    private int windowSeconds = 60;
}
