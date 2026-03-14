package com.ljj.campusrun.activity;

import com.ljj.campusrun.common.ApiResponse;
import com.ljj.campusrun.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    public ApiResponse<Object> listActivities() {
        return ApiResponse.ok(activityService.listActivities());
    }

    @GetMapping("/tutorials")
    public ApiResponse<Object> listTutorials() {
        return ApiResponse.ok(activityService.listTutorials());
    }

    @PostMapping("/{activityId}/register")
    public ApiResponse<Object> register(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long activityId) {
        return ApiResponse.ok("报名成功", activityService.register(principal.user().getId(), activityId));
    }
}
