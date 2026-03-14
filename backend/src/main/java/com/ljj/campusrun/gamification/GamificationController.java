package com.ljj.campusrun.gamification;

import com.ljj.campusrun.common.ApiResponse;
import com.ljj.campusrun.security.AppUserPrincipal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;
    private final RankingService rankingService;

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.ok(gamificationService.getOverview(principal.user().getId()));
    }

    @GetMapping("/ranking")
    public ApiResponse<Object> ranking() {
        return ApiResponse.ok(rankingService.getDistanceRanking());
    }
}
