package com.ljj.campusrun.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ljj.campusrun.config.WeChatAuthProperties;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeChatBindingTicketService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final WeChatAuthProperties properties;

    public String issue(WeChatSessionInfo sessionInfo) {
        String ticket = UUID.randomUUID().toString();
        try {
            redisTemplate.opsForValue().set(key(ticket), objectMapper.writeValueAsString(sessionInfo),
                    Duration.ofMinutes(properties.getTicketTtlMinutes()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("微信绑定票据生成失败");
        }
        return ticket;
    }

    public WeChatSessionInfo load(String ticket) {
        String raw = redisTemplate.opsForValue().get(key(ticket));
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("微信绑定已失效，请重新发起登录");
        }
        try {
            return objectMapper.readValue(raw, WeChatSessionInfo.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("微信绑定票据解析失败");
        }
    }

    public void clear(String ticket) {
        redisTemplate.delete(key(ticket));
    }

    private String key(String ticket) {
        return "campus-run:wechat:bind:" + ticket;
    }
}
