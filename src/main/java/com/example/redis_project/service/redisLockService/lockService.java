package com.example.redis_project.service.redisLockService;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class lockService {
    private final StringRedisTemplate redisTemplate;

    public lockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Attempts to acquire a lock.
     * @param lockKey The name of the lock (e.g., "lock:product:1")
     * @param lockValue A unique identifier for the thread holding the lock (e.g., "thread-xyz")
     * @param timeoutMilliseconds How long the lock should live before auto-expiring (Deadlock prevention)
     * @return true if lock acquired, false if someone else holds it.
     */
    public boolean acquireLock(String lockKey, String lockValue, long timeoutMilliseconds) {
        // This is the Spring Boot equivalent of the Redis command: SET lockKey lockValue NX PX timeoutMilliseconds
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofMillis(timeoutMilliseconds));

        return Boolean.TRUE.equals(success);
    }

    /**
     * Releases the lock.
     * @param lockKey The name of the lock
     */
    public void releaseLock(String lockKey) {
        redisTemplate.delete(lockKey);
    }
}
