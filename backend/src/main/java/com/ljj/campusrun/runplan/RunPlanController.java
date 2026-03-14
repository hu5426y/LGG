package com.ljj.campusrun.runplan;

import com.ljj.campusrun.common.ApiResponse;
import com.ljj.campusrun.security.AppUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/run-plans")
@RequiredArgsConstructor
public class RunPlanController {

    private final RunPlanService runPlanService;

    @GetMapping
    public ApiResponse<Object> templates() {
        return ApiResponse.ok(runPlanService.listTemplates());
    }

    @GetMapping("/current")
    public ApiResponse<Object> current(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.ok(runPlanService.getCurrentPlan(principal.user().getId()));
    }

    @PostMapping("/select")
    public ApiResponse<Object> select(@AuthenticationPrincipal AppUserPrincipal principal,
                                      @Valid @RequestBody SelectRunPlanRequest request) {
        return ApiResponse.ok("跑步计划已切换", runPlanService.selectPlan(principal.user().getId(), request.templateId()));
    }
}
