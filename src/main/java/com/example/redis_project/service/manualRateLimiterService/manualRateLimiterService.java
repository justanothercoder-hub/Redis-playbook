package com.example.redis_project.service.manualRateLimiterService;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class manualRateLimiterService {

        private final StringRedisTemplate redisTemplate;

        public manualRateLimiterService(StringRedisTemplate redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        public boolean allowRequest(String apiKey) {
            String key = "rate:limit:" + apiKey;

            // 1. INCR the counter (Redis creates the key with value 1 if it doesn't exist)
            Long currentCount = redisTemplate.opsForValue().increment(key);

            // 2. If it's the very first request, set the TTL window to 60 seconds
            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(60));
            }

            // 3. Return true if they are under the limit (e.g., max 5 requests)
            return currentCount != null && currentCount <= 5;
        }
    }
