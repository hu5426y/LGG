package com.ljj.campusrun.auth;

import com.ljj.campusrun.config.WeChatAuthProperties;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class WeChatAuthClient {

    private final WeChatAuthProperties properties;
    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.weixin.qq.com")
            .build();

    public WeChatSessionInfo exchangeCode(String code) {
        if (!properties.isEnabled()) {
            throw new IllegalArgumentException("微信登录尚未启用");
        }
        if (isBlank(properties.getAppId()) || isBlank(properties.getAppSecret())) {
            throw new IllegalArgumentException("微信登录配置未完成");
        }
        Map<?, ?> response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/sns/jscode2session")
                            .queryParam("appid", properties.getAppId())
                            .queryParam("secret", properties.getAppSecret())
                            .queryParam("js_code", code)
                            .queryParam("grant_type", "authorization_code")
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("微信登录服务暂时不可用，请稍后重试");
        }

        if (response == null) {
            throw new IllegalArgumentException("微信登录失败，请稍后重试");
        }
        Object errorCode = response.get("errcode");
        if (errorCode instanceof Number number && number.intValue() != 0) {
            Object errorMessage = response.get("errmsg");
            throw new IllegalArgumentException("微信登录失败：" + (errorMessage == null ? "code2Session 调用异常" : errorMessage));
        }
        String openid = asString(response.get("openid"));
        if (isBlank(openid)) {
            throw new IllegalArgumentException("微信登录失败：未获取到 openid");
        }
        return new WeChatSessionInfo(
                openid,
                asString(response.get("unionid")),
                asString(response.get("session_key"))
        );
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
