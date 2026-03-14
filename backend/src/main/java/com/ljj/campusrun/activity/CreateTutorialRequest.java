package com.ljj.campusrun.activity;

import jakarta.validation.constraints.NotBlank;

public record CreateTutorialRequest(@NotBlank String title, String coverUrl, String summary, @NotBlank String content, Boolean published) {
}
