package com.ljj.campusrun.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    void wechatLoginShouldReturnBindingTicketWhenUserNotBound() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        WeChatAuthClient weChatAuthClient = mock(WeChatAuthClient.class);
        WeChatBindingTicketService ticketService = mock(WeChatBindingTicketService.class);
        SimpleRateLimiterService rateLimiterService = mock(SimpleRateLimiterService.class);
        LoginRateLimitProperties properties = new LoginRateLimitProperties();
        AuditLogService auditLogService = mock(AuditLogService.class);
        AuthService authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtTokenProvider,
                weChatAuthClient,
                ticketService,
                rateLimiterService,
                properties,
                auditLogService
        );

        when(weChatAuthClient.exchangeCode("code-1")).thenReturn(new WeChatSessionInfo("openid-1", null, "session"));
        when(userRepository.findByWechatOpenid("openid-1")).thenReturn(Optional.empty());
        when(ticketService.issue(any(WeChatSessionInfo.class))).thenReturn("ticket-1");

        WeChatLoginResponse response = authService.wechatLogin(new WeChatCodeLoginRequest("code-1"), "127.0.0.1");

        assertFalse(response.bound());
        assertEquals("ticket-1", response.wechatTicket());
    }

    @Test
    void bindWechatShouldRejectAdminAccount() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        WeChatAuthClient weChatAuthClient = mock(WeChatAuthClient.class);
        WeChatBindingTicketService ticketService = mock(WeChatBindingTicketService.class);
        SimpleRateLimiterService rateLimiterService = mock(SimpleRateLimiterService.class);
        LoginRateLimitProperties properties = new LoginRateLimitProperties();
        AuditLogService auditLogService = mock(AuditLogService.class);
        AuthService authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtTokenProvider,
                weChatAuthClient,
                ticketService,
                rateLimiterService,
                properties,
                auditLogService
        );

        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setPassword("{noop}admin123");

        when(ticketService.load("ticket-admin")).thenReturn(new WeChatSessionInfo("openid-admin", null, "session"));
        when(userRepository.findByWechatOpenid("openid-admin")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.bindWechat(new WeChatBindRequest("ticket-admin", "admin", "admin123"), "127.0.0.1"));

        assertTrue(exception.getMessage().contains("仅学生账号支持绑定微信登录"));
    }

    @Test
    void bindWechatShouldPersistBindingAndReturnToken() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        WeChatAuthClient weChatAuthClient = mock(WeChatAuthClient.class);
        WeChatBindingTicketService ticketService = mock(WeChatBindingTicketService.class);
        SimpleRateLimiterService rateLimiterService = mock(SimpleRateLimiterService.class);
        LoginRateLimitProperties properties = new LoginRateLimitProperties();
        AuditLogService auditLogService = mock(AuditLogService.class);
        AuthService authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtTokenProvider,
                weChatAuthClient,
                ticketService,
                rateLimiterService,
                properties,
                auditLogService
        );

        User student = new User();
        student.setId(2L);
        student.setUsername("20230001");
        student.setStudentNo("20230001");
        student.setDisplayName("张晨");
        student.setRole(UserRole.STUDENT);
        student.setStatus(UserStatus.ACTIVE);
        student.setPassword("{noop}123456");

        when(ticketService.load("ticket-2")).thenReturn(new WeChatSessionInfo("openid-2", "unionid-2", "session"));
        when(userRepository.findByWechatOpenid("openid-2")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("20230001")).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("123456", "{noop}123456")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateToken(student)).thenReturn("jwt-token");

        WeChatLoginResponse response = authService.bindWechat(
                new WeChatBindRequest("ticket-2", "20230001", "123456"),
                "127.0.0.1"
        );

        assertTrue(response.bound());
        assertEquals("jwt-token", response.token());
        assertEquals("openid-2", student.getWechatOpenid());
        verify(userRepository).save(student);
    }
}
