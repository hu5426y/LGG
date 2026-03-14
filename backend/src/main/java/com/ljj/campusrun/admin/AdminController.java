package com.ljj.campusrun.admin;

import com.ljj.campusrun.activity.CreateActivityRequest;
import com.ljj.campusrun.activity.CreateTutorialRequest;
import com.ljj.campusrun.common.ApiResponse;
import com.ljj.campusrun.security.AppUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ApiResponse<Object> dashboard() {
        return ApiResponse.ok(adminService.dashboard());
    }

    @GetMapping("/students")
    public ApiResponse<Object> students() {
        return ApiResponse.ok(adminService.listStudents());
    }

    @GetMapping("/students/{studentId}")
    public ApiResponse<Object> student(@PathVariable Long studentId) {
        return ApiResponse.ok(adminService.getStudent(studentId));
    }

    @PostMapping("/students/import")
    public ApiResponse<Object> importStudents(@AuthenticationPrincipal AppUserPrincipal principal,
                                              @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("学生导入完成", adminService.importStudents(principal.user().getId(), file));
    }

    @PostMapping("/students/{studentId}/status")
    public ApiResponse<Object> updateStudentStatus(@AuthenticationPrincipal AppUserPrincipal principal,
                                                   @PathVariable Long studentId,
                                                   @Valid @RequestBody UpdateStudentStatusRequest request) {
        return ApiResponse.ok("学生状态已更新", adminService.updateStudentStatus(principal.user().getId(), studentId, request));
    }

    @GetMapping("/posts/pending")
    public ApiResponse<Object> pendingPosts() {
        return ApiResponse.ok(adminService.listPendingPosts());
    }

    @PostMapping("/posts/{postId}/review")
    public ApiResponse<Object> reviewPost(@AuthenticationPrincipal AppUserPrincipal principal,
                                          @PathVariable Long postId,
                                          @Valid @RequestBody ReviewPostRequest request) {
        return ApiResponse.ok("审核完成", adminService.reviewPost(principal.user().getId(), postId, request));
    }

    @GetMapping("/badges")
    public ApiResponse<Object> badges() {
        return ApiResponse.ok(adminService.listBadges());
    }

    @PostMapping("/badges")
    public ApiResponse<Object> createBadge(@AuthenticationPrincipal AppUserPrincipal principal,
                                           @Valid @RequestBody CreateBadgeRequest request) {
        return ApiResponse.ok("勋章已保存", adminService.saveBadge(principal.user().getId(), request));
    }

    @PostMapping("/activities")
    public ApiResponse<Object> createActivity(@AuthenticationPrincipal AppUserPrincipal principal,
                                              @Valid @RequestBody CreateActivityRequest request) {
        return ApiResponse.ok("活动已保存", adminService.saveActivity(principal.user().getId(), request));
    }

    @PostMapping("/tutorials")
    public ApiResponse<Object> createTutorial(@AuthenticationPrincipal AppUserPrincipal principal,
                                              @Valid @RequestBody CreateTutorialRequest request) {
        return ApiResponse.ok("教程已保存", adminService.saveTutorial(principal.user().getId(), request));
    }

    @GetMapping("/logs")
    public ApiResponse<Object> logs() {
        return ApiResponse.ok(adminService.listLogs());
    }

    @GetMapping("/metrics/overview")
    public ApiResponse<Object> metricsOverview() {
        return ApiResponse.ok(adminService.metricsOverview());
    }

    @GetMapping("/metrics/trends")
    public ApiResponse<Object> metricsTrends() {
        return ApiResponse.ok(adminService.metricsTrends());
    }
}
