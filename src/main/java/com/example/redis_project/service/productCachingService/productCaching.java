package com.example.redis_project.service.productCachingService;

import com.example.redis_project.entity.product;
import com.example.redis_project.service.productCoreService.productService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class productCaching {

    private final productService coreService;

    public productCaching(productService coreService) {
        this.coreService = coreService;
    }

    public product createProduct(product product) {
        return coreService.createProduct(product);
    }

    @Cacheable(value = "productCache", key = "#id")
    public product getProductById(Long id) {
        return coreService.getProductById(id);
    }

    @CacheEvict(value = "productCache", key = "#id")
    public product updateProductStock(Long id, int newStockQuantity) {
        return coreService.updateProductStock(id, newStockQuantity);
    }

    @CacheEvict(value = "productCache", key = "#id")
    public void deleteProduct(Long id) {
        coreService.deleteProduct(id);
    }
}