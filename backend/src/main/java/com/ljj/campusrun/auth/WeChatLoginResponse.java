package com.ljj.campusrun.auth;

public record WeChatLoginResponse(
        boolean bound,
        String wechatTicket,
        String token,
        Long userId,
        String username,
        String displayName,
        String role,
        String message
) {
}
