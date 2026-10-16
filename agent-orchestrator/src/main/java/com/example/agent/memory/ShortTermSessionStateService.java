package com.example.agent.memory;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class ShortTermSessionStateService {

    private static final Duration STATE_TTL = Duration.ofMinutes(30);
    private final RedisTemplate<String, Object> redisTemplate;

    public ShortTermSessionStateService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String stateKey(String customerId, String sessionId) {
        return "agent:state:" + customerId + ":" + sessionId;
    }

    public Map<String, Object> updateSessionState(String customerId, String sessionId, Map<String, Object> newFields) {
        String key = stateKey(customerId, sessionId);
        redisTemplate.opsForHash().putAll(key, newFields);
        redisTemplate.expire(key, STATE_TTL);
        return getSessionState(customerId, sessionId);
    }

    public Map<String, Object> getSessionState(String customerId, String sessionId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(stateKey(customerId, sessionId));
        Map<String, Object> state = new HashMap<>();
        entries.forEach((k, v) -> state.put(String.valueOf(k), v));
        return state;
    }

    public void clearSessionState(String customerId, String sessionId) {
        redisTemplate.delete(stateKey(customerId, sessionId));
    }
}