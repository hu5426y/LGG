package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.FeedPost;
import com.ljj.campusrun.domain.enums.ReviewStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedPostRepository extends JpaRepository<FeedPost, Long> {

    List<FeedPost> findTop20ByReviewStatusOrderByFeaturedDescCreatedAtDesc(ReviewStatus reviewStatus);

    List<FeedPost> findTop10ByReviewStatusOrderByLikeCountDescCreatedAtDesc(ReviewStatus reviewStatus);

    List<FeedPost> findByReviewStatusOrderByCreatedAtDesc(ReviewStatus reviewStatus);
}
