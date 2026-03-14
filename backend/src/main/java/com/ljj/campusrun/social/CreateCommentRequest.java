package com.ljj.campusrun.social;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(@NotBlank(message = "评论内容不能为空") String content) {
}
