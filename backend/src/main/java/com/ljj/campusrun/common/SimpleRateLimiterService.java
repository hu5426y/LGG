package com.ljj.campusrun.common;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SimpleRateLimiterService {

    private final ConcurrentHashMap<String, CounterWindow> counters = new ConcurrentHashMap<>();

    public void checkLimit(String key, int maxAttempts, int windowSeconds, String message) {
        long now = Instant.now().getEpochSecond();
        CounterWindow window = counters.compute(key, (ignored, existing) -> {
            if (existing == null || existing.expiresAt() <= now) {
                return new CounterWindow(new AtomicInteger(1), now + windowSeconds);
            }
            existing.counter().incrementAndGet();
            return existing;
        });
        if (window.counter().get() > maxAttempts) {
            throw new TooManyRequestsException(message);
        }
    }

    private record CounterWindow(AtomicInteger counter, long expiresAt) {
    }
}
