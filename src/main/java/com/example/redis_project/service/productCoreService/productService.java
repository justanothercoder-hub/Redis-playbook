package com.example.redis_project.service.productCoreService;

import com.example.redis_project.entity.product;
import com.example.redis_project.repository.productRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class productService {

    private final productRepository productRepository;

    public productService(productRepository productRepository) {
        this.productRepository = productRepository;
    }

    public product createProduct(product product) {
        return productRepository.save(product);
    }

    @Cacheable(value = "productCache", key = "#id")
    public product getProductById(Long id) {
        simulateSlowDatabaseQuery();

        Optional<product> product = productRepository.findById(id);
        return product.orElseThrow(() -> new RuntimeException("Product not found!"));
    }

    @CacheEvict(value = "productCache", key = "#id")
    public product updateProductStock(Long id, int newStockQuantity) {

        product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found!"));


        product.setStockQuantity(newStockQuantity);

        return productRepository.save(product);
    }

    @CacheEvict(value = "productCache", key = "#id")
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found!");
        }
        productRepository.deleteById(id);
    }

    private void simulateSlowDatabaseQuery() {
        try {
            System.out.println("Fetching from MySQL Database... this is slow!");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

