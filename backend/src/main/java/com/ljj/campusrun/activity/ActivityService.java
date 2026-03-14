package com.ljj.campusrun.activity;

import com.ljj.campusrun.domain.entity.Activity;
import com.ljj.campusrun.domain.entity.ActivityRegistration;
import com.ljj.campusrun.domain.entity.Tutorial;
import com.ljj.campusrun.domain.enums.ActivityStatus;
import com.ljj.campusrun.domain.enums.RegistrationStatus;
import com.ljj.campusrun.repository.ActivityRegistrationRepository;
import com.ljj.campusrun.repository.ActivityRepository;
import com.ljj.campusrun.repository.TutorialRepository;
import com.ljj.campusrun.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final TutorialRepository tutorialRepository;
    private final UserRepository userRepository;

    public Object listActivities() {
        return activityRepository.findByStatusOrderByStartTimeAsc(ActivityStatus.PUBLISHED);
    }

    public Object listTutorials() {
        return tutorialRepository.findTop5ByPublishedTrueOrderByCreatedAtDesc();
    }

    @Transactional
    public Object register(Long userId, Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("活动不存在"));
        if (activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new IllegalArgumentException("当前活动不可报名");
        }
        if (activity.getRegistrationDeadline().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("报名已截止");
        }
        activityRegistrationRepository.findByActivityIdAndUserId(activityId, userId)
                .ifPresent(existing -> {
                    if (existing.getStatus() == RegistrationStatus.REGISTERED) {
                        throw new IllegalArgumentException("请勿重复报名");
                    }
                });
        long count = activityRegistrationRepository.countByActivityIdAndStatus(activityId, RegistrationStatus.REGISTERED);
        if (activity.getMaxCapacity() > 0 && count >= activity.getMaxCapacity()) {
            throw new IllegalArgumentException("活动人数已满");
        }
        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivity(activity);
        registration.setUser(userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在")));
        registration.setStatus(RegistrationStatus.REGISTERED);
        ActivityRegistration saved = activityRegistrationRepository.save(registration);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", saved.getId());
        data.put("activityId", saved.getActivity().getId());
        data.put("userId", saved.getUser().getId());
        data.put("status", saved.getStatus());
        return data;
    }

    @Transactional
    public Activity saveActivity(CreateActivityRequest request) {
        Activity activity = new Activity();
        activity.setTitle(request.title());
        activity.setLocation(request.location());
        activity.setStartTime(request.startTime());
        activity.setEndTime(request.endTime());
        activity.setRegistrationDeadline(request.registrationDeadline());
        activity.setCoverUrl(request.coverUrl());
        activity.setDescription(request.description());
        activity.setMaxCapacity(request.maxCapacity() == null ? 0 : request.maxCapacity());
        activity.setStatus(request.status() == null ? ActivityStatus.PUBLISHED : request.status());
        return activityRepository.save(activity);
    }

    @Transactional
    public Tutorial saveTutorial(CreateTutorialRequest request) {
        Tutorial tutorial = new Tutorial();
        tutorial.setTitle(request.title());
        tutorial.setCoverUrl(request.coverUrl());
        tutorial.setSummary(request.summary());
        tutorial.setContent(request.content());
        tutorial.setPublished(request.published() == null || request.published());
        return tutorialRepository.save(tutorial);
    }
}
