package com.ljj.campusrun.auth;

import com.ljj.campusrun.admin.AuditLogService;
import com.ljj.campusrun.common.SimpleRateLimiterService;
import com.ljj.campusrun.config.LoginRateLimitProperties;
import com.ljj.campusrun.domain.entity.User;
import com.ljj.campusrun.domain.enums.UserRole;
import com.ljj.campusrun.domain.enums.UserStatus;
import com.ljj.campusrun.repository.UserRepository;
import com.ljj.campusrun.security.JwtTokenProvider;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final WeChatAuthClient weChatAuthClient;
    private final WeChatBindingTicketService weChatBindingTicketService;
    private final SimpleRateLimiterService rateLimiterService;
    private final LoginRateLimitProperties loginRateLimitProperties;
    private final AuditLogService auditLogService;

    public LoginResponse login(LoginRequest request, String remoteAddr) {
        rateLimiterService.checkLimit("auth:password:" + normalizeIp(remoteAddr),
                loginRateLimitProperties.getMaxAttempts(),
                loginRateLimitProperties.getWindowSeconds(),
                "登录过于频繁，请稍后再试");
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        validateUserCanLogin(user);
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        auditLogService.log(user, "LOGIN_PASSWORD", "sys_user", String.valueOf(user.getId()), "账号密码登录");
        return toLoginResponse(user);
    }

    public WeChatLoginResponse wechatLogin(WeChatCodeLoginRequest request, String remoteAddr) {
        rateLimiterService.checkLimit("auth:wechat:" + normalizeIp(remoteAddr),
                loginRateLimitProperties.getMaxAttempts(),
                loginRateLimitProperties.getWindowSeconds(),
                "微信登录过于频繁，请稍后再试");
        WeChatSessionInfo sessionInfo = weChatAuthClient.exchangeCode(request.code());
        return userRepository.findByWechatOpenid(sessionInfo.openid())
                .map(user -> {
                    validateUserCanLogin(user);
                    auditLogService.log(user, "LOGIN_WECHAT", "sys_user", String.valueOf(user.getId()), "微信登录");
                    LoginResponse response = toLoginResponse(user);
                    return new WeChatLoginResponse(
                            true,
                            null,
                            response.token(),
                            response.userId(),
                            response.username(),
                            response.displayName(),
                            response.role(),
                            "微信登录成功"
                    );
                })
                .orElseGet(() -> new WeChatLoginResponse(
                        false,
                        weChatBindingTicketService.issue(sessionInfo),
                        null,
                        null,
                        null,
                        null,
                        null,
                        "当前微信尚未绑定学生账号"
                ));
    }

    public WeChatLoginResponse bindWechat(WeChatBindRequest request, String remoteAddr) {
        rateLimiterService.checkLimit("auth:wechat-bind:" + normalizeIp(remoteAddr),
                loginRateLimitProperties.getMaxAttempts(),
                loginRateLimitProperties.getWindowSeconds(),
                "绑定操作过于频繁，请稍后再试");
        WeChatSessionInfo sessionInfo = weChatBindingTicketService.load(request.wechatTicket());
        userRepository.findByWechatOpenid(sessionInfo.openid()).ifPresent(existing -> {
            throw new IllegalArgumentException("该微信已绑定其他账号");
        });
        User user = userRepository.findByUsername(request.username())
                .or(() -> userRepository.findByStudentNo(request.username()))
                .orElseThrow(() -> new IllegalArgumentException("学号或密码错误"));
        validateUserCanLogin(user);
        if (user.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("仅学生账号支持绑定微信登录");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("学号或密码错误");
        }
        if (user.getWechatOpenid() != null && !user.getWechatOpenid().equals(sessionInfo.openid())) {
            throw new IllegalArgumentException("该账号已绑定其他微信，请联系管理员重置");
        }
        user.setWechatOpenid(sessionInfo.openid());
        user.setWechatUnionid(sessionInfo.unionid());
        user.setWechatBoundAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        weChatBindingTicketService.clear(request.wechatTicket());
        auditLogService.log(saved, "BIND_WECHAT", "sys_user", String.valueOf(saved.getId()), "绑定微信登录");
        LoginResponse response = toLoginResponse(saved);
        return new WeChatLoginResponse(
                true,
                null,
                response.token(),
                response.userId(),
                response.username(),
                response.displayName(),
                response.role(),
                "微信绑定成功"
        );
    }

    private LoginResponse toLoginResponse(User user) {
        String token = jwtTokenProvider.generateToken(user);
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getDisplayName(), user.getRole().name());
    }

    private void validateUserCanLogin(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("账号已被禁用");
        }
    }

    private String normalizeIp(String remoteAddr) {
        return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
    }
}
