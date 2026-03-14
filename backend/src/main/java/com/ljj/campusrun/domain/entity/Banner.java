package com.ljj.campusrun.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "banner")
public class Banner extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String title;

    @Column(length = 128)
    private String subtitle;

    @Column(nullable = false, length = 255)
    private String imageUrl;

    @Column(nullable = false, length = 32)
    private String linkType;

    @Column(length = 128)
    private String linkTarget;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean active = true;
}
