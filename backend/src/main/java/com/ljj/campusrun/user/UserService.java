package com.ljj.campusrun.user;

import com.ljj.campusrun.domain.enums.RunState;
import com.ljj.campusrun.repository.RunSessionRepository;
import com.ljj.campusrun.repository.UserBadgeRepository;
import com.ljj.campusrun.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final RunSessionRepository runSessionRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentUserProfile(Long userId) {
        var user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("profile", mapProfile(user));
        data.put("badgeCount", userBadgeRepository.findByUserIdOrderByGrantedAtDesc(userId).size());
        data.put("finishedRuns", runSessionRepository.countByUserIdAndState(userId, RunState.FINISHED));
        List<Map<String, Object>> recentRuns = runSessionRepository.findTop20ByUserIdAndStateOrderByStartedAtDesc(userId, RunState.FINISHED).stream()
                .limit(5)
                .map(this::mapRecentRun)
                .toList();
        data.put("recentRuns", recentRuns);
        return data;
    }

    private Map<String, Object> mapProfile(com.ljj.campusrun.domain.entity.User user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("displayName", user.getDisplayName());
        profile.put("studentNo", user.getStudentNo() == null ? "" : user.getStudentNo());
        profile.put("college", user.getCollege() == null ? "" : user.getCollege());
        profile.put("className", user.getClassName() == null ? "" : user.getClassName());
        profile.put("avatarUrl", user.getAvatarUrl() == null ? "" : user.getAvatarUrl());
        profile.put("bio", user.getBio() == null ? "" : user.getBio());
        profile.put("points", user.getPoints());
        profile.put("levelValue", user.getLevelValue());
        profile.put("totalDistanceKm", user.getTotalDistanceKm());
        profile.put("totalDurationSeconds", user.getTotalDurationSeconds());
        return profile;
    }

    private Map<String, Object> mapRecentRun(com.ljj.campusrun.domain.entity.RunSession run) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", run.getId());
        data.put("distanceKm", run.getDistanceKm());
        data.put("durationSeconds", run.getDurationSeconds());
        data.put("avgPaceSeconds", run.getAvgPaceSeconds());
        data.put("calories", run.getCalories());
        data.put("finishedAt", run.getFinishedAt());
        return data;
    }
}
