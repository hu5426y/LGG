package com.ljj.campusrun.auth;

public record WeChatSessionInfo(
        String openid,
        String unionid,
        String sessionKey
) {
}
