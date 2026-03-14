package com.ljj.campusrun.auth;

import jakarta.validation.constraints.NotBlank;

public record WeChatCodeLoginRequest(
        @NotBlank(message = "微信登录 code 不能为空") String code
) {
}
