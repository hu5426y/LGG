package com.ljj.campusrun.social;

import jakarta.validation.constraints.NotBlank;

public record ReportPostRequest(@NotBlank(message = "举报原因不能为空") String reason) {
}
