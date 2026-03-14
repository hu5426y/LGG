package com.ljj.campusrun.social;

import jakarta.validation.constraints.NotBlank;

public record CreateClubMessageRequest(@NotBlank(message = "消息不能为空") String content) {
}
