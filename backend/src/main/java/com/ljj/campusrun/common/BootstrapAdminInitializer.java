package com.ljj.campusrun.common;

import com.ljj.campusrun.config.BootstrapAdminProperties;
import com.ljj.campusrun.domain.entity.User;
import com.ljj.campusrun.domain.enums.UserRole;
import com.ljj.campusrun.domain.enums.UserStatus;
import com.ljj.campusrun.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BootstrapAdminInitializer {

    private final BootstrapAdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner bootstrapAdminRunner() {
        return args -> {
            if (!properties.isEnabled() || userRepository.existsByRole(UserRole.ADMIN)) {
                return;
            }
            if (isBlank(properties.getUsername()) || isBlank(properties.getPassword())) {
                throw new IllegalStateException("启用 bootstrap admin 时必须提供用户名和密码");
            }
            User admin = new User();
            admin.setUsername(properties.getUsername().trim());
            admin.setPassword(passwordEncoder.encode(properties.getPassword()));
            admin.setDisplayName(isBlank(properties.getDisplayName()) ? "系统管理员" : properties.getDisplayName().trim());
            admin.setCollege(isBlank(properties.getCollege()) ? "平台运营中心" : properties.getCollege().trim());
            admin.setClassName("管理组");
            admin.setRole(UserRole.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            userRepository.save(admin);
            log.info("Bootstrap admin {} created", admin.getUsername());
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
