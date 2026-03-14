package com.ljj.campusrun.auth;

import jakarta.validation.constraints.NotBlank;

public record WeChatBindRequest(
        @NotBlank(message = "绑定票据不能为空") String wechatTicket,
        @NotBlank(message = "学号或用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password
) {
}
