package com.example.redis_project.controller;

import com.example.redis_project.service.manualRateLimiterService.manualRateLimiterService;
import com.example.redis_project.entity.product;
import com.example.redis_project.service.productCachingService.productCaching;
import com.example.redis_project.service.redisAnalyticsService.redisAnalytics;
import com.example.redis_project.service.redisLockService.lockService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class productController {

    private final productCaching cachingService;
    private final manualRateLimiterService rateLimiterService;
    private final redisAnalytics analyticsService;
    private final lockService lockEngine;

    // 3. INJECT INTO CONSTRUCTOR
    public productController(productCaching cachingService,
                             manualRateLimiterService rateLimiterService,
                             redisAnalytics analyticsService,
                             lockService lockEngine) {
        this.cachingService = cachingService;
        this.rateLimiterService = rateLimiterService;
        this.analyticsService = analyticsService;
        this.lockEngine = lockEngine;
    }

    @PostMapping
    public product createProduct(@RequestBody product product) {
        // Save ONCE
        product savedProduct = cachingService.createProduct(product);
        analyticsService.enqueueNotification("EMAIL_JOB: Send promo for new " + savedProduct.getName());
        // Return the one we already saved
        return savedProduct;
    }

    // /////////////////////DISTRIBUTED LOCKING EXAMPLE (BUY PRODUCT)/////////////////////
    @PostMapping("/{id}/buy")
    public ResponseEntity<String> buyProductSafely(@PathVariable Long id) {
        String lockKey = "lock:product:" + id;
        // In a real system, this would be a unique UUID per request so you only delete YOUR lock.
        String lockValue = "user-thread-" + System.currentTimeMillis();

        // 1. ATTEMPT TO ACQUIRE THE LOCK (Wait up to 5 seconds if someone else has it)
        boolean acquired = false;
        int retries = 0;

        while (!acquired && retries < 10) { // Try 10 times, pausing slightly between attempts
            // FIXED: Using lockEngine object instead of lockService class
            acquired = lockEngine.acquireLock(lockKey, lockValue, 5000);
            if (!acquired) {
                try {
                    Thread.sleep(100); // Wait 100ms and try grabbing the key again
                    retries++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (!acquired) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("System is busy. Too many people trying to buy this item right now.");
        }

        // 2. CRITICAL SECTION (You hold the key! It's safe to touch the database)
        try {
            // A. Check Stock
            product item = cachingService.getProductById(id);
            if (item == null || item.getStockQuantity() <= 0) {
                return ResponseEntity.badRequest().body("Sorry, out of stock!");
            }

            // B. Simulate slow database/processing time (Optional, but helps with testing)
            try { Thread.sleep(500); } catch (InterruptedException e) {}

            // C. Update Stock
            cachingService.updateProductStock(id, item.getStockQuantity() - 1);

            return ResponseEntity.ok("Successfully purchased! Remaining stock: " + (item.getStockQuantity() - 1));

        } finally {
            // 3. RELEASE THE LOCK
            // FIXED: Using lockEngine object instead of lockService class
            lockEngine.releaseLock(lockKey);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        // Hardcode a user identifier for testing.
        String testUserId = "user1";

        // Check the rate limit before hitting the cache or database
        if (!rateLimiterService.allowRequest(testUserId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("HTTP 429: Too Many Requests. You are blocked for the rest of the minute.");
        }

        // If allowed, fetch the product
        product fetchedProduct = cachingService.getProductById(id);

        // 4. LOG ANALYTICS IF PRODUCT EXISTS
        if (fetchedProduct != null) {
            // Log to the List (Recently Viewed History)
            analyticsService.logProductView(testUserId, fetchedProduct.getName());
            // Log to the ZSet (Most Popular Leaderboard)
            analyticsService.incrementProductPopularity(fetchedProduct.getName());
        }

        // Return with a 200 OK status
        return ResponseEntity.ok(fetchedProduct);
    }

    @PutMapping("/{id}/stock")
    public product updateStock(@PathVariable Long id, @RequestParam int quantity) {
        return cachingService.updateProductStock(id, quantity);
    }

    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable Long id) {
        cachingService.deleteProduct(id);
        return "Deleted successfully from Database and Cache.";
    }
}