package kr.hhplus.be.server.monitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Redis 모니터링 컨트롤러
 * Redis Insight와 함께 사용하여 실시간 모니터링
 */
@RestController
@RequestMapping("/api/monitoring/redis")
@RequiredArgsConstructor
public class RedisMonitoringController {

    @Qualifier("cacheRedisTemplate")
    private final RedisTemplate<String, Object> cacheRedisTemplate;

    /**
     * 현재 활성 락 상태 조회
     */
    @GetMapping("/locks")
    public Map<String, Object> getActiveLocks() {
        Set<String> lockKeys = cacheRedisTemplate.keys("ecommerce:lock:*");
        
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> locks = new HashMap<>();
        
        if (lockKeys != null) {
            for (String key : lockKeys) {
                String value = (String) cacheRedisTemplate.opsForValue().get(key);
                Long ttl = cacheRedisTemplate.getExpire(key);
                
                locks.put(key, Map.of(
                    "value", value != null ? value : "null",
                    "ttl", ttl,
                    "isActive", value != null && ttl > 0
                ));
            }
        }
        
        result.put("activeLockCount", locks.size());
        result.put("locks", locks);
        result.put("timestamp", LocalDateTime.now());
        
        return result;
    }

    /**
     * 현재 캐시 상태 조회
     */
    @GetMapping("/cache")
    public Map<String, Object> getCacheStatus() {
        Map<String, Object> result = new HashMap<>();
        
        // 상품 캐시 통계
        Set<String> productCacheKeys = cacheRedisTemplate.keys("products::*");
        Set<String> popularProductsKeys = cacheRedisTemplate.keys("popular-products::*");
        Set<String> manualCacheKeys = cacheRedisTemplate.keys("ecommerce::product::*");
        
        result.put("productCacheCount", productCacheKeys != null ? productCacheKeys.size() : 0);
        result.put("popularProductsCacheCount", popularProductsKeys != null ? popularProductsKeys.size() : 0);
        result.put("manualCacheCount", manualCacheKeys != null ? manualCacheKeys.size() : 0);
        result.put("timestamp", LocalDateTime.now());
        
        return result;
    }

    /**
     * Redis 키 패턴별 통계
     */
    @GetMapping("/stats")
    public Map<String, Object> getRedisStats() {
        Map<String, Object> result = new HashMap<>();
        
        // 패턴별 키 개수 조회
        Map<String, Integer> keyPatterns = new HashMap<>();
        
        String[] patterns = {
            "products::*",
            "popular-products::*", 
            "ecommerce::product::*",
            "ecommerce:lock:*",
            "ecommerce:metrics:*"
        };
        
        for (String pattern : patterns) {
            Set<String> keys = cacheRedisTemplate.keys(pattern);
            keyPatterns.put(pattern, keys != null ? keys.size() : 0);
        }
        
        result.put("keyPatterns", keyPatterns);
        result.put("timestamp", LocalDateTime.now());
        
        return result;
    }

    /**
     * 테스트용 캐시 데이터 생성 
     */
    @GetMapping("/test-data")
    public Map<String, Object> createTestData() {
        // 테스트용 캐시 키들 생성
        for (int i = 1; i <= 5; i++) {
            String productKey = "products::" + i;
            cacheRedisTemplate.opsForValue().set(productKey, "test-product-" + i);
            
            String lockKey = "ecommerce:lock:test:" + i;
            cacheRedisTemplate.opsForValue().set(lockKey, "test-lock-value-" + i, 
                java.time.Duration.ofMinutes(1));
        }
        
        return Map.of(
            "message", "테스트 데이터가 생성되었습니다",
            "productKeys", 5,
            "lockKeys", 5,
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * 테스트 데이터 정리
     */
    @GetMapping("/cleanup")
    public Map<String, Object> cleanupTestData() {
        // 테스트 키들 정리
        Set<String> testKeys = cacheRedisTemplate.keys("*test*");
        if (testKeys != null && !testKeys.isEmpty()) {
            cacheRedisTemplate.delete(testKeys);
        }
        
        return Map.of(
            "message", "테스트 데이터가 정리되었습니다",
            "deletedKeys", testKeys != null ? testKeys.size() : 0,
            "timestamp", LocalDateTime.now()
        );
    }
}