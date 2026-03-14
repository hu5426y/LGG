package com.ljj.campusrun.social;

import jakarta.validation.constraints.NotBlank;

public record CreatePostRequest(@NotBlank(message = "动态内容不能为空") String content, String imageUrls) {
}
