package com.ljj.campusrun.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "club")
public class Club extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 255)
    private String slogan;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Integer memberCount = 0;

    @Column(nullable = false)
    private Boolean active = true;
}
