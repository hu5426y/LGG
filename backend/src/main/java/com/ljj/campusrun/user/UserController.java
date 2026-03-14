package com.ljj.campusrun.user;

import com.ljj.campusrun.common.ApiResponse;
import com.ljj.campusrun.security.AppUserPrincipal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.ok(userService.getCurrentUserProfile(principal.user().getId()));
    }
}
