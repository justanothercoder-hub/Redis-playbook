package com.example.redis_project.worker;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class notificationWorker {

    private final StringRedisTemplate redisTemplate;

    public notificationWorker(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // This tells Spring Boot to run this exact method every 3000 milliseconds (3 seconds)
    @Scheduled(fixedDelay = 3000)
    public void processQueue() {
        String queueName = "queue:notifications";

        // RPOP: Take the oldest item off the right side of the list.
        // IMPORTANT: The second this line executes, the item is DELETED from Redis.
        String job = redisTemplate.opsForList().rightPop(queueName);

        if (job != null) {
            System.out.println("\n=================================================");
            System.out.println("[WORKER WOKE UP] Time: " + LocalDateTime.now());
            System.out.println("[PROCESSING JOB] " + job);
            System.out.println("=================================================\n");

            // Note for the interview: If your server's power cord was unplugged
            // right at this exact millisecond, this job is lost forever.
            // This is "At Most Once" delivery!
        }
    }
}