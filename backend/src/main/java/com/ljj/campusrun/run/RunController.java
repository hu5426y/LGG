package com.ljj.campusrun.run;

import com.ljj.campusrun.common.ApiResponse;
import com.ljj.campusrun.security.AppUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runs")
@RequiredArgsConstructor
public class RunController {

    private final RunService runService;

    @PostMapping("/start")
    public ApiResponse<Object> start(@AuthenticationPrincipal AppUserPrincipal principal,
                                     @RequestBody(required = false) StartRunRequest request) {
        return ApiResponse.ok("开始跑步成功", runService.start(principal.user().getId(), request));
    }

    @PostMapping("/{runId}/pause")
    public ApiResponse<Object> pause(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long runId) {
        return ApiResponse.ok("已暂停", runService.pause(principal.user().getId(), runId));
    }

    @PostMapping("/{runId}/resume")
    public ApiResponse<Object> resume(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long runId) {
        return ApiResponse.ok("已继续", runService.resume(principal.user().getId(), runId));
    }

    @PostMapping("/{runId}/finish")
    public ApiResponse<Object> finish(@AuthenticationPrincipal AppUserPrincipal principal,
                                      @PathVariable Long runId,
                                      @Valid @RequestBody FinishRunRequest request) {
        return ApiResponse.ok("跑步完成", runService.finish(principal.user().getId(), runId, request));
    }

    @GetMapping("/{runId}")
    public ApiResponse<Object> detail(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long runId) {
        return ApiResponse.ok(runService.detail(principal.user().getId(), runId));
    }

    @GetMapping("/current")
    public ApiResponse<Object> current(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.ok(runService.current(principal.user().getId()));
    }

    @GetMapping("/history")
    public ApiResponse<Object> history(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.ok(runService.history(principal.user().getId()));
    }

    @PostMapping("/{runId}/discard")
    public ApiResponse<Object> discard(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long runId) {
        return ApiResponse.ok("已放弃当前跑步", runService.discard(principal.user().getId(), runId));
    }
}
