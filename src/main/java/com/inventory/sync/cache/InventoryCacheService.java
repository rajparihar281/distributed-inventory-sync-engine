package com.inventory.sync.cache;

import com.inventory.sync.dto.InventoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.inventory.ttl-seconds:300}")
    private long ttlSeconds;

    private static final String KEY_PREFIX = "inventory:";

    private String buildKey(Long productId, Long warehouseId) {
        return KEY_PREFIX + productId + ":" + warehouseId;
    }

    public Optional<InventoryResponse> get(Long productId, Long warehouseId) {
        String key = buildKey(productId, warehouseId);
        try {
            String cachedJson = redisTemplate.opsForValue().get(key);
            if (cachedJson != null && !cachedJson.isBlank()) {
                InventoryResponse response = objectMapper.readValue(cachedJson, InventoryResponse.class);
                log.info("[REDIS CACHE HIT] Key: {}", key);
                return Optional.of(response);
            }
        } catch (Exception ex) {
            log.warn("[REDIS ERROR] Fallback to DB on read for key: {}. Error: {}", key, ex.getMessage());
        }
        log.info("[REDIS CACHE MISS] Key: {}", key);
        return Optional.empty();
    }

    public void put(Long productId, Long warehouseId, InventoryResponse response) {
        String key = buildKey(productId, warehouseId);
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
            log.info("[REDIS CACHE PUT] Key: {} with TTL: {}s", key, ttlSeconds);
        } catch (Exception ex) {
            log.warn("[REDIS ERROR] Failed to populate cache for key: {}. Error: {}", key, ex.getMessage());
        }
    }

    public void evict(Long productId, Long warehouseId) {
        String key = buildKey(productId, warehouseId);
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.info("[REDIS CACHE EVICT] Key: {} (Deleted: {})", key, deleted);
        } catch (Exception ex) {
            log.warn("[REDIS ERROR] Failed to evict cache for key: {}. Error: {}", key, ex.getMessage());
        }
    }
}