package com.ljj.campusrun.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tutorial")
public class Tutorial extends BaseEntity {

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 255)
    private String coverUrl;

    @Column(length = 255)
    private String summary;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false)
    private Boolean published = true;
}
