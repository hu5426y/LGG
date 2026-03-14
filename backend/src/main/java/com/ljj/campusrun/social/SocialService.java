package com.ljj.campusrun.social;

import com.ljj.campusrun.domain.entity.Club;
import com.ljj.campusrun.domain.entity.ClubMember;
import com.ljj.campusrun.domain.entity.ClubMessage;
import com.ljj.campusrun.domain.entity.FeedPost;
import com.ljj.campusrun.domain.entity.PostComment;
import com.ljj.campusrun.domain.entity.PostLike;
import com.ljj.campusrun.domain.entity.PostReport;
import com.ljj.campusrun.domain.entity.User;
import com.ljj.campusrun.domain.enums.ReviewStatus;
import com.ljj.campusrun.repository.ClubMemberRepository;
import com.ljj.campusrun.repository.ClubMessageRepository;
import com.ljj.campusrun.repository.ClubRepository;
import com.ljj.campusrun.repository.FeedPostRepository;
import com.ljj.campusrun.repository.PostCommentRepository;
import com.ljj.campusrun.repository.PostLikeRepository;
import com.ljj.campusrun.repository.PostReportRepository;
import com.ljj.campusrun.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocialService {

    private static final Set<String> SENSITIVE_WORDS = Set.of("赌博", "外挂", "刷单", "兼职返利", "黄色");

    private final FeedPostRepository feedPostRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostReportRepository postReportRepository;
    private final ClubRepository clubRepository;
    private final ClubMessageRepository clubMessageRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Object> listPosts() {
        return feedPostRepository.findTop20ByReviewStatusOrderByFeaturedDescCreatedAtDesc(ReviewStatus.APPROVED).stream()
                .map(this::mapPost)
                .toList();
    }

    @Transactional
    public Object createPost(Long userId, CreatePostRequest request) {
        User user = getUser(userId);
        FeedPost post = new FeedPost();
        post.setUser(user);
        post.setContent(request.content());
        post.setImageUrls(request.imageUrls());
        String riskTags = SENSITIVE_WORDS.stream()
                .filter(request.content()::contains)
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
        post.setRiskTags(riskTags);
        post.setReviewStatus(riskTags == null ? ReviewStatus.APPROVED : ReviewStatus.PENDING);
        return mapPost(feedPostRepository.save(post));
    }

    @Transactional
    public Object likePost(Long userId, Long postId) {
        FeedPost post = getPost(postId);
        postLikeRepository.findByPostIdAndUserId(postId, userId).ifPresentOrElse(existing -> {
            throw new IllegalArgumentException("请勿重复点赞");
        }, () -> {
            PostLike like = new PostLike();
            like.setPost(post);
            like.setUser(getUser(userId));
            postLikeRepository.save(like);
            post.setLikeCount(post.getLikeCount() + 1);
            feedPostRepository.save(post);
        });
        return mapPost(post);
    }

    @Transactional(readOnly = true)
    public List<Object> listComments(Long postId) {
        return postCommentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(this::mapComment)
                .toList();
    }

    @Transactional
    public Object comment(Long userId, Long postId, CreateCommentRequest request) {
        FeedPost post = getPost(postId);
        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setUser(getUser(userId));
        comment.setContent(request.content());
        post.setCommentCount(post.getCommentCount() + 1);
        feedPostRepository.save(post);
        return mapComment(postCommentRepository.save(comment));
    }

    @Transactional
    public Object report(Long userId, Long postId, ReportPostRequest request) {
        FeedPost post = getPost(postId);
        PostReport report = new PostReport();
        report.setPost(post);
        report.setReporter(getUser(userId));
        report.setReason(request.reason());
        post.setReportCount(post.getReportCount() + 1);
        if (post.getReportCount() >= 2) {
            post.setReviewStatus(ReviewStatus.PENDING);
        }
        feedPostRepository.save(post);
        PostReport saved = postReportRepository.save(report);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", saved.getId());
        data.put("reason", saved.getReason());
        data.put("postId", postId);
        return data;
    }

    @Transactional(readOnly = true)
    public Object listClubs() {
        return clubRepository.findByActiveTrueOrderByMemberCountDesc().stream()
                .map(this::mapClub)
                .toList();
    }

    @Transactional(readOnly = true)
    public Object listClubMessages(Long userId, Long clubId) {
        ensureClubMember(clubId, userId);
        return clubMessageRepository.findTop30ByClubIdOrderByCreatedAtDesc(clubId).stream()
                .map(this::mapClubMessage)
                .toList();
    }

    @Transactional
    public Object sendClubMessage(Long userId, Long clubId, CreateClubMessageRequest request) {
        ensureClubMember(clubId, userId);
        ClubMessage message = new ClubMessage();
        message.setClub(clubRepository.findById(clubId).orElseThrow(() -> new IllegalArgumentException("跑步小队不存在")));
        message.setUser(getUser(userId));
        message.setContent(request.content());
        ClubMessage saved = clubMessageRepository.save(message);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", saved.getId());
        data.put("content", saved.getContent());
        data.put("createdAt", saved.getCreatedAt());
        return data;
    }

    @Transactional
    public Object joinClub(Long userId, Long clubId) {
        User user = getUser(userId);
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new IllegalArgumentException("跑步小队不存在"));
        clubMemberRepository.findByClubIdAndUserId(clubId, userId).ifPresentOrElse(member -> {
            if (Boolean.TRUE.equals(member.getActive())) {
                throw new IllegalArgumentException("已经加入该小队");
            }
            member.setActive(true);
            member.setJoinedAt(LocalDateTime.now());
            clubMemberRepository.save(member);
        }, () -> {
            ClubMember member = new ClubMember();
            member.setClub(club);
            member.setUser(user);
            member.setRole("MEMBER");
            member.setJoinedAt(LocalDateTime.now());
            clubMemberRepository.save(member);
        });
        refreshClubMemberCount(club);
        return mapClub(club);
    }

    @Transactional
    public Object leaveClub(Long userId, Long clubId) {
        ClubMember member = clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new IllegalArgumentException("尚未加入该小队"));
        member.setActive(false);
        clubMemberRepository.save(member);
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new IllegalArgumentException("跑步小队不存在"));
        refreshClubMemberCount(club);
        return Map.of("clubId", clubId, "left", true);
    }

    @Transactional(readOnly = true)
    public Object listClubMembers(Long userId, Long clubId) {
        ensureClubMember(clubId, userId);
        return clubMemberRepository.findByClubIdAndActiveTrueOrderByJoinedAtAsc(clubId).stream()
                .map(member -> Map.<String, Object>of(
                        "id", member.getId(),
                        "role", member.getRole(),
                        "joinedAt", member.getJoinedAt(),
                        "user", mapUserSummary(member.getUser())
                ))
                .toList();
    }

    private FeedPost getPost(Long postId) {
        return feedPostRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("动态不存在"));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    private void ensureClubMember(Long clubId, Long userId) {
        if (!clubMemberRepository.existsByClubIdAndUserIdAndActiveTrue(clubId, userId)) {
            throw new IllegalArgumentException("请先加入跑步小队");
        }
    }

    private void refreshClubMemberCount(Club club) {
        int memberCount = clubMemberRepository.findByClubIdAndActiveTrueOrderByJoinedAtAsc(club.getId()).size();
        club.setMemberCount(memberCount);
        clubRepository.save(club);
    }

    private Object mapPost(FeedPost post) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", post.getId());
        data.put("content", post.getContent());
        data.put("imageUrls", post.getImageUrls() == null ? "" : post.getImageUrls());
        data.put("likeCount", post.getLikeCount());
        data.put("commentCount", post.getCommentCount());
        data.put("reportCount", post.getReportCount());
        data.put("riskTags", post.getRiskTags() == null ? "" : post.getRiskTags());
        data.put("featured", post.getFeatured());
        data.put("reviewStatus", post.getReviewStatus());
        data.put("createdAt", post.getCreatedAt());
        data.put("user", mapUserSummary(post.getUser()));
        return data;
    }

    private Object mapComment(PostComment comment) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", comment.getId());
        data.put("content", comment.getContent());
        data.put("createdAt", comment.getCreatedAt());
        data.put("user", mapUserSummary(comment.getUser()));
        return data;
    }

    private Map<String, Object> mapClubMessage(ClubMessage message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", message.getId());
        data.put("content", message.getContent());
        data.put("createdAt", message.getCreatedAt());
        data.put("clubId", message.getClub().getId());
        data.put("user", mapUserSummary(message.getUser()));
        return data;
    }

    private Map<String, Object> mapClub(Club club) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", club.getId());
        data.put("name", club.getName());
        data.put("slogan", club.getSlogan() == null ? "" : club.getSlogan());
        data.put("description", club.getDescription() == null ? "" : club.getDescription());
        data.put("memberCount", club.getMemberCount());
        return data;
    }

    private Map<String, Object> mapUserSummary(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("displayName", user.getDisplayName());
        return data;
    }
}
