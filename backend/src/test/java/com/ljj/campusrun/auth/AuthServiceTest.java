package com.ljj.campusrun.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ljj.campusrun.admin.AuditLogService;
import com.ljj.campusrun.common.SimpleRateLimiterService;
import com.ljj.campusrun.config.LoginRateLimitProperties;
import com.ljj.campusrun.domain.entity.User;
import com.ljj.campusrun.domain.enums.UserRole;
import com.ljj.campusrun.domain.enums.UserStatus;
import com.ljj.campusrun.repository.UserRepository;
import com.ljj.campusrun.security.JwtTokenProvider;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    @Test
    void loginShouldReturnTokenForActiveUser() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        SimpleRateLimiterService rateLimiterService = mock(SimpleRateLimiterService.class);
        LoginRateLimitProperties properties = new LoginRateLimitProperties();
        AuditLogService auditLogService = mock(AuditLogService.class);
        AuthService authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtTokenProvider,
                rateLimiterService,
                properties,
                auditLogService
        );

        User student = new User();
        student.setId(2L);
        student.setUsername("20230001");
        student.setDisplayName("张晨");
        student.setRole(UserRole.STUDENT);
        student.setStatus(UserStatus.ACTIVE);
        student.setPassword("{noop}123456");

        when(userRepository.findByUsername("20230001")).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("123456", "{noop}123456")).thenReturn(true);
        when(jwtTokenProvider.generateToken(student)).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("20230001", "123456"), "127.0.0.1");

        assertEquals("jwt-token", response.token());
        assertEquals("20230001", response.username());
        verify(auditLogService).log(student, "LOGIN_PASSWORD", "sys_user", "2", "账号密码登录");
    }

    @Test
    void loginShouldRejectDisabledUser() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        SimpleRateLimiterService rateLimiterService = mock(SimpleRateLimiterService.class);
        LoginRateLimitProperties properties = new LoginRateLimitProperties();
        AuditLogService auditLogService = mock(AuditLogService.class);
        AuthService authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtTokenProvider,
                rateLimiterService,
                properties,
                auditLogService
        );

        User student = new User();
        student.setUsername("20230001");
        student.setRole(UserRole.STUDENT);
        student.setStatus(UserStatus.DISABLED);
        student.setPassword("{noop}123456");

        when(userRepository.findByUsername("20230001")).thenReturn(Optional.of(student));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.login(new LoginRequest("20230001", "123456"), "127.0.0.1"));

        assertEquals("账号已被禁用", exception.getMessage());
    }
}
