package com.ljj.campusrun.social;

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
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;

    @GetMapping("/posts")
    public ApiResponse<Object> posts() {
        return ApiResponse.ok(socialService.listPosts());
    }

    @PostMapping("/posts")
    public ApiResponse<Object> createPost(@AuthenticationPrincipal AppUserPrincipal principal,
                                          @Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.ok("动态发布成功", socialService.createPost(principal.user().getId(), request));
    }

    @PostMapping("/posts/{postId}/like")
    public ApiResponse<Object> like(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long postId) {
        return ApiResponse.ok("点赞成功", socialService.likePost(principal.user().getId(), postId));
    }

    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<Object> comments(@PathVariable Long postId) {
        return ApiResponse.ok(socialService.listComments(postId));
    }

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<Object> comment(@AuthenticationPrincipal AppUserPrincipal principal,
                                       @PathVariable Long postId,
                                       @Valid @RequestBody CreateCommentRequest request) {
        return ApiResponse.ok("评论成功", socialService.comment(principal.user().getId(), postId, request));
    }

    @PostMapping("/posts/{postId}/report")
    public ApiResponse<Object> report(@AuthenticationPrincipal AppUserPrincipal principal,
                                      @PathVariable Long postId,
                                      @Valid @RequestBody ReportPostRequest request) {
        return ApiResponse.ok("举报已提交", socialService.report(principal.user().getId(), postId, request));
    }

    @GetMapping("/clubs")
    public ApiResponse<Object> clubs() {
        return ApiResponse.ok(socialService.listClubs());
    }

    @GetMapping("/clubs/{clubId}/messages")
    public ApiResponse<Object> clubMessages(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long clubId) {
        return ApiResponse.ok(socialService.listClubMessages(principal.user().getId(), clubId));
    }

    @PostMapping("/clubs/{clubId}/messages")
    public ApiResponse<Object> sendClubMessage(@AuthenticationPrincipal AppUserPrincipal principal,
                                               @PathVariable Long clubId,
                                               @Valid @RequestBody CreateClubMessageRequest request) {
        return ApiResponse.ok("消息已发送", socialService.sendClubMessage(principal.user().getId(), clubId, request));
    }

    @PostMapping("/clubs/{clubId}/join")
    public ApiResponse<Object> joinClub(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long clubId) {
        return ApiResponse.ok("已加入跑步小队", socialService.joinClub(principal.user().getId(), clubId));
    }

    @PostMapping("/clubs/{clubId}/leave")
    public ApiResponse<Object> leaveClub(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long clubId) {
        return ApiResponse.ok("已退出跑步小队", socialService.leaveClub(principal.user().getId(), clubId));
    }

    @GetMapping("/clubs/{clubId}/members")
    public ApiResponse<Object> clubMembers(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long clubId) {
        return ApiResponse.ok(socialService.listClubMembers(principal.user().getId(), clubId));
    }
}
