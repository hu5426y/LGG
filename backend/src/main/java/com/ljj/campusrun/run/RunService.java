package com.ljj.campusrun.run;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ljj.campusrun.admin.AuditLogService;
import com.ljj.campusrun.config.RunValidationProperties;
import com.ljj.campusrun.domain.entity.RunSession;
import com.ljj.campusrun.domain.entity.User;
import com.ljj.campusrun.domain.enums.RunState;
import com.ljj.campusrun.gamification.GamificationService;
import com.ljj.campusrun.gamification.RankingService;
import com.ljj.campusrun.repository.RunSessionRepository;
import com.ljj.campusrun.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RunService {

    private final RunSessionRepository runSessionRepository;
    private final UserRepository userRepository;
    private final GamificationService gamificationService;
    private final RankingService rankingService;
    private final RunValidationProperties runValidationProperties;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    @Transactional
    public Object start(Long userId, StartRunRequest request) {
        runSessionRepository.findFirstByUserIdAndStateInOrderByStartedAtDesc(userId, List.of(RunState.RUNNING, RunState.PAUSED))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("存在未结束的跑步记录，请先完成");
                });
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        boolean simulated = request != null && Boolean.TRUE.equals(request.simulated());
        if (simulated && !runValidationProperties.isAllowSimulatedRuns()) {
            throw new IllegalArgumentException("当前环境不允许模拟跑步");
        }
        RunSession runSession = new RunSession();
        runSession.setUser(user);
        runSession.setState(RunState.RUNNING);
        runSession.setStartedAt(LocalDateTime.now());
        String source = request == null || request.source() == null || request.source().isBlank() ? "wechat-miniapp" : request.source();
        runSession.setSource(simulated ? source + ":simulated" : source);
        runSession.setDeviceModel(request == null ? null : request.deviceModel());
        runSession.setDevicePlatform(request == null ? null : request.devicePlatform());
        runSession.setClientVersion(request == null ? null : request.clientVersion());
        runSession.setRouteSnapshot("[]");
        runSession.setRoutePointCount(0);
        RunSession saved = runSessionRepository.save(runSession);
        auditLogService.log(user, "RUN_START", "run_session", String.valueOf(saved.getId()), saved.getSource());
        return mapRunSummary(saved);
    }

    @Transactional
    public Object pause(Long userId, Long runId) {
        RunSession runSession = getOwnedRun(runId, userId);
        if (runSession.getState() != RunState.RUNNING) {
            throw new IllegalArgumentException("当前跑步状态不可暂停");
        }
        runSession.setState(RunState.PAUSED);
        runSession.setPausedAt(LocalDateTime.now());
        RunSession saved = runSessionRepository.save(runSession);
        auditLogService.log(runSession.getUser(), "RUN_PAUSE", "run_session", String.valueOf(saved.getId()), "暂停跑步");
        return mapRunSummary(saved);
    }

    @Transactional
    public Object resume(Long userId, Long runId) {
        RunSession runSession = getOwnedRun(runId, userId);
        if (runSession.getState() != RunState.PAUSED) {
            throw new IllegalArgumentException("当前跑步状态不可继续");
        }
        runSession.setState(RunState.RUNNING);
        runSession.setPausedAt(null);
        RunSession saved = runSessionRepository.save(runSession);
        auditLogService.log(runSession.getUser(), "RUN_RESUME", "run_session", String.valueOf(saved.getId()), "继续跑步");
        return mapRunSummary(saved);
    }

    @Transactional
    public Object finish(Long userId, Long runId, FinishRunRequest request) {
        RunSession runSession = getOwnedRun(runId, userId);
        if (runSession.getState() == RunState.FINISHED) {
            throw new IllegalArgumentException("该跑步记录已结束");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        validateFinishRequest(runSession, request);
        runSession.setState(RunState.FINISHED);
        runSession.setFinishedAt(LocalDateTime.now());
        runSession.setDistanceKm(request.distanceKm());
        runSession.setDurationSeconds(request.durationSeconds());
        runSession.setStepCount(request.stepCount());
        runSession.setRouteSnapshot(serializeRoutePoints(request.routePoints()));
        runSession.setRoutePointCount(request.routePoints().size());
        runSession.setAvgPaceSeconds(RunMetricCalculator.averagePaceSeconds(request.distanceKm(), request.durationSeconds()));
        runSession.setCalories(RunMetricCalculator.calories(request.distanceKm()));
        RunSession savedRun = runSessionRepository.save(runSession);

        user.setTotalDistanceKm(user.getTotalDistanceKm() + request.distanceKm());
        user.setTotalDurationSeconds(user.getTotalDurationSeconds() + request.durationSeconds());
        gamificationService.applyRunRewards(user, savedRun);
        userRepository.save(user);
        rankingService.evictDistanceRanking();
        auditLogService.log(user, "RUN_FINISH", "run_session", String.valueOf(savedRun.getId()),
                "distanceKm=" + request.distanceKm() + ",durationSeconds=" + request.durationSeconds());
        return mapRunSummary(savedRun);
    }

    @Transactional(readOnly = true)
    public Object detail(Long userId, Long runId) {
        RunSession runSession = getOwnedRun(runId, userId);
        Map<String, Object> data = new LinkedHashMap<>(mapRunSummary(runSession));
        data.put("routePoints", deserializeRoutePoints(runSession.getRouteSnapshot()));
        return data;
    }

    @Transactional(readOnly = true)
    public Object current(Long userId) {
        return runSessionRepository.findFirstByUserIdAndStateInOrderByStartedAtDesc(userId, List.of(RunState.RUNNING, RunState.PAUSED))
                .map(runSession -> {
                    Map<String, Object> data = new LinkedHashMap<>(mapRunSummary(runSession));
                    data.put("routePoints", deserializeRoutePoints(runSession.getRouteSnapshot()));
                    return data;
                })
                .orElse(null);
    }

    public List<Map<String, Object>> history(Long userId) {
        return runSessionRepository.findTop20ByUserIdAndStateOrderByStartedAtDesc(userId, RunState.FINISHED)
                .stream()
                .map(this::mapRunSummary)
                .toList();
    }

    @Transactional
    public Object discard(Long userId, Long runId) {
        RunSession runSession = getOwnedRun(runId, userId);
        if (runSession.getState() == RunState.FINISHED) {
            throw new IllegalArgumentException("已结束的跑步记录不能放弃");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        runSessionRepository.delete(runSession);
        auditLogService.log(user, "RUN_DISCARD", "run_session", String.valueOf(runId), "放弃未结束跑步");
        return Map.of("id", runId, "discarded", true);
    }

    private RunSession getOwnedRun(Long runId, Long userId) {
        RunSession runSession = runSessionRepository.findByIdAndUserId(runId, userId)
                .orElseThrow(() -> new IllegalArgumentException("跑步记录不存在"));
        return runSession;
    }

    private Map<String, Object> mapRunSummary(RunSession runSession) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", runSession.getId());
        data.put("state", runSession.getState());
        data.put("startedAt", runSession.getStartedAt());
        data.put("pausedAt", runSession.getPausedAt());
        data.put("finishedAt", runSession.getFinishedAt());
        data.put("durationSeconds", runSession.getDurationSeconds());
        data.put("distanceKm", runSession.getDistanceKm());
        data.put("avgPaceSeconds", runSession.getAvgPaceSeconds());
        data.put("calories", runSession.getCalories());
        data.put("stepCount", runSession.getStepCount());
        data.put("routePointCount", runSession.getRoutePointCount());
        data.put("source", runSession.getSource() == null ? "" : runSession.getSource());
        data.put("deviceModel", runSession.getDeviceModel() == null ? "" : runSession.getDeviceModel());
        data.put("devicePlatform", runSession.getDevicePlatform() == null ? "" : runSession.getDevicePlatform());
        data.put("clientVersion", runSession.getClientVersion() == null ? "" : runSession.getClientVersion());
        return data;
    }

    private void validateFinishRequest(RunSession runSession, FinishRunRequest request) {
        if (request.durationSeconds() < runValidationProperties.getMinDurationSeconds()) {
            throw new IllegalArgumentException("跑步时长过短，未达到有效记录门槛");
        }
        if (request.distanceKm() < runValidationProperties.getMinDistanceKm()) {
            throw new IllegalArgumentException("跑步距离过短，未达到有效记录门槛");
        }
        if (request.routePoints() == null || request.routePoints().size() < runValidationProperties.getMinRoutePoints()) {
            throw new IllegalArgumentException("轨迹点过少，请开启定位后重试");
        }
        double averageSpeedKmh = request.distanceKm() * 3600D / request.durationSeconds();
        if (averageSpeedKmh > runValidationProperties.getMaxSpeedKmh()) {
            throw new IllegalArgumentException("跑步速度异常，记录未通过校验");
        }
        if (runSession.getSource() != null
                && runSession.getSource().contains("simulated")
                && !runValidationProperties.isAllowSimulatedRuns()) {
            throw new IllegalArgumentException("当前环境不允许提交模拟跑步记录");
        }

        List<RunRoutePointRequest> routePoints = request.routePoints();
        long lastTimestamp = -1L;
        for (int i = 0; i < routePoints.size(); i++) {
            RunRoutePointRequest current = routePoints.get(i);
            if (lastTimestamp >= 0 && current.timestamp() <= lastTimestamp) {
                throw new IllegalArgumentException("轨迹时间戳异常，请重新开始跑步");
            }
            if (i > 0) {
                RunRoutePointRequest previous = routePoints.get(i - 1);
                long deltaMillis = current.timestamp() - previous.timestamp();
                if (deltaMillis > 0) {
                    double segmentDistanceMeters = RunMetricCalculator.distanceMeters(
                            previous.latitude(), previous.longitude(), current.latitude(), current.longitude());
                    double segmentSpeedKmh = segmentDistanceMeters / deltaMillis * 3.6D * 1000D;
                    if (segmentSpeedKmh > runValidationProperties.getMaxSpeedKmh()) {
                        throw new IllegalArgumentException("轨迹速度异常，记录未通过校验");
                    }
                }
            }
            lastTimestamp = current.timestamp();
        }
    }

    private String serializeRoutePoints(List<RunRoutePointRequest> routePoints) {
        try {
            return objectMapper.writeValueAsString(routePoints == null ? List.of() : routePoints);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("轨迹数据序列化失败");
        }
    }

    private List<Map<String, Object>> deserializeRoutePoints(String routeSnapshot) {
        if (routeSnapshot == null || routeSnapshot.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(routeSnapshot, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
