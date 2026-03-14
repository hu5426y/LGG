package com.ljj.campusrun.auth;

import com.ljj.campusrun.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok("登录成功", authService.login(request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/wechat/login")
    public ApiResponse<WeChatLoginResponse> wechatLogin(@Valid @RequestBody WeChatCodeLoginRequest request,
                                                        HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.wechatLogin(request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/wechat/bind")
    public ApiResponse<WeChatLoginResponse> wechatBind(@Valid @RequestBody WeChatBindRequest request,
                                                       HttpServletRequest httpRequest) {
        return ApiResponse.ok("绑定成功", authService.bindWechat(request, httpRequest.getRemoteAddr()));
    }
}
