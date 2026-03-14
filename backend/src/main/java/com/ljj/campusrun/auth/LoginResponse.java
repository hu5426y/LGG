package com.ljj.campusrun.auth;

public record LoginResponse(
        String token,
        Long userId,
        String username,
        String displayName,
        String role
) {
}
