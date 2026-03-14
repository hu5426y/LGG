package com.ljj.campusrun.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "campusrun.bootstrap-admin")
public class BootstrapAdminProperties {

    private boolean enabled;
    private String username;
    private String password;
    private String displayName;
    private String college;
}
