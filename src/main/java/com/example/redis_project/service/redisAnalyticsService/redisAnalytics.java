package com.example.redis_project.service.redisAnalyticsService;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class redisAnalytics {

    private final StringRedisTemplate redisTemplate;

    public redisAnalytics(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 1. HASH: Store many fields for a User Profile
    public void updateUserProfile(String userId, String name, String email) {
        String key = "user:profile:" + userId;
        redisTemplate.opsForHash().put(key, "name", name);
        redisTemplate.opsForHash().put(key, "email", email);
        redisTemplate.opsForHash().put(key, "lastLogin", String.valueOf(System.currentTimeMillis()));
    }

    // 2. LIST: Maintain a 'Last 5 Products Viewed' log
    public void logProductView(String userId, String productName) {
        String key = "user:history:" + userId;
        // Push to the front of the list
        redisTemplate.opsForList().leftPush(key, productName);
        // Keep only the latest 5 items (Trim the list)
        redisTemplate.opsForList().trim(key, 0, 4);
    }

    // 3. ZSET: Create a 'Most Popular Products' Leaderboard
    public void incrementProductPopularity(String productName) {
        String key = "leaderboard:products";
        // Increment the 'score' of this product by 1
        redisTemplate.opsForZSet().incrementScore(key, productName, 1);
    }

    public Set<ZSetOperations.TypedTuple<String>> getTopProducts() {
        String key = "leaderboard:products";
        // Get top 3 products with their scores
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, 2);
    }

    // 4. QUEUE: Push a background job to the list
    public void enqueueNotification(String message) {
        String queueName = "queue:notifications";
        redisTemplate.opsForList().leftPush(queueName, message);
    }
}