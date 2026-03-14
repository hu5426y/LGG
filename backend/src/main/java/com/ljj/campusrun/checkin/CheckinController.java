package com.ljj.campusrun.checkin;

import com.ljj.campusrun.common.ApiResponse;
import com.ljj.campusrun.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkins")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    @GetMapping("/today")
    public ApiResponse<Object> today(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.ok(checkinService.getToday(principal.user().getId()));
    }

    @GetMapping("/history")
    public ApiResponse<Object> history(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.ok(checkinService.getHistory(principal.user().getId()));
    }
}
