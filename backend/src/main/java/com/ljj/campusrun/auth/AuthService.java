package com.ljj.campusrun.auth;

import com.ljj.campusrun.admin.AuditLogService;
import com.ljj.campusrun.common.SimpleRateLimiterService;
import com.ljj.campusrun.config.LoginRateLimitProperties;
import com.ljj.campusrun.domain.entity.User;
import com.ljj.campusrun.domain.enums.UserRole;
import com.ljj.campusrun.domain.enums.UserStatus;
import com.ljj.campusrun.repository.UserRepository;
import com.ljj.campusrun.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
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
