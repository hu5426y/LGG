package com.ljj.campusrun.gamification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ljj.campusrun.domain.enums.UserRole;
import com.ljj.campusrun.repository.UserRepository;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${campusrun.ranking.ttl-minutes}")
    private long ttlMinutes;

    public List<Map<String, Object>> getDistanceRanking() {
        String key = "campus-run:ranking:distance";
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<>() {
                });
            } catch (Exception ignored) {
            }
        }

        List<Map<String, Object>> result = userRepository.findByRole(UserRole.STUDENT).stream()
                .sorted(Comparator.comparingDouble(user -> -user.getTotalDistanceKm()))
                .limit(10)
                .map(user -> Map.<String, Object>of(
                        "userId", user.getId(),
                        "displayName", user.getDisplayName(),
                        "college", user.getCollege(),
                        "distanceKm", user.getTotalDistanceKm(),
                        "points", user.getPoints(),
                        "levelValue", user.getLevelValue()
                ))
                .toList();

        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(result), Duration.ofMinutes(ttlMinutes));
        } catch (Exception ignored) {
        }
        return result;
    }

    public void evictDistanceRanking() {
        redisTemplate.delete("campus-run:ranking:distance");
    }
}
