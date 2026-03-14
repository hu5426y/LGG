package com.ljj.campusrun.domain.entity;

import com.ljj.campusrun.domain.enums.UserRole;
import com.ljj.campusrun.domain.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(nullable = false, length = 128)
    private String password;

    @Column(nullable = false, length = 64)
    private String displayName;

    @Column(length = 64)
    private String studentNo;

    @Column(length = 64)
    private String college;

    @Column(length = 64)
    private String className;

    @Column(length = 255)
    private String avatarUrl;

    @Column(length = 16)
    private String gender;

    @Column(length = 255)
    private String bio;

    @Column(length = 128, unique = true)
    private String wechatOpenid;

    @Column(length = 128)
    private String wechatUnionid;

    private LocalDateTime wechatBoundAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserStatus status;

    @Column(nullable = false)
    private Integer totalDurationSeconds = 0;

    @Column(nullable = false)
    private Double totalDistanceKm = 0D;

    @Column(nullable = false)
    private Integer points = 0;

    @Column(nullable = false)
    private Integer levelValue = 1;
}
