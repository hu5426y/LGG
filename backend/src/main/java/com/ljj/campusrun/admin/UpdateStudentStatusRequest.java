package com.ljj.campusrun.admin;

import jakarta.validation.constraints.NotBlank;

public record UpdateStudentStatusRequest(
        @NotBlank(message = "状态不能为空") String status
) {
}
