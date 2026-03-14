package com.ljj.campusrun.admin;

import jakarta.validation.constraints.NotBlank;

public record ReviewPostRequest(@NotBlank String action, String remark) {
}
