package com.ljj.campusrun.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "campusrun.wechat")
public class WeChatAuthProperties {

    private boolean enabled;
    private String appId;
    private String appSecret;
    private long ticketTtlMinutes = 10;
}
