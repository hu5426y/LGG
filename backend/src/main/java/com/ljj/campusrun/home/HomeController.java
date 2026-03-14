package com.ljj.campusrun.home;

import com.ljj.campusrun.common.ApiResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    public ApiResponse<Map<String, Object>> getHomeData() {
        return ApiResponse.ok(homeService.getHomeData());
    }
}
