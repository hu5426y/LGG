package com.ljj.campusrun.home;

import com.ljj.campusrun.domain.enums.ActivityStatus;
import com.ljj.campusrun.domain.enums.ReviewStatus;
import com.ljj.campusrun.gamification.RankingService;
import com.ljj.campusrun.repository.ActivityRepository;
import com.ljj.campusrun.repository.BannerRepository;
import com.ljj.campusrun.repository.FeedPostRepository;
import com.ljj.campusrun.repository.TutorialRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final BannerRepository bannerRepository;
    private final FeedPostRepository feedPostRepository;
    private final ActivityRepository activityRepository;
    private final TutorialRepository tutorialRepository;
    private final RankingService rankingService;

    @Transactional(readOnly = true)
    public Map<String, Object> getHomeData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("banners", bannerRepository.findByActiveTrueOrderBySortOrderAsc());
        List<Map<String, Object>> hotPosts = feedPostRepository.findTop10ByReviewStatusOrderByLikeCountDescCreatedAtDesc(ReviewStatus.APPROVED)
                .stream()
                .map(this::mapHotPost)
                .toList();
        data.put("hotPosts", hotPosts);
        data.put("recommendedActivities", activityRepository.findByStatusOrderByStartTimeAsc(ActivityStatus.PUBLISHED));
        data.put("tutorials", tutorialRepository.findTop5ByPublishedTrueOrderByCreatedAtDesc());
        data.put("leaderboard", rankingService.getDistanceRanking());
        return data;
    }

    private Map<String, Object> mapHotPost(com.ljj.campusrun.domain.entity.FeedPost post) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", post.getId());
        data.put("content", post.getContent());
        data.put("likeCount", post.getLikeCount());
        data.put("commentCount", post.getCommentCount());
        data.put("featured", post.getFeatured());
        data.put("createdAt", post.getCreatedAt());
        data.put("user", mapUserSummary(post.getUser()));
        return data;
    }

    private Map<String, Object> mapUserSummary(com.ljj.campusrun.domain.entity.User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("displayName", user.getDisplayName());
        return data;
    }
}
