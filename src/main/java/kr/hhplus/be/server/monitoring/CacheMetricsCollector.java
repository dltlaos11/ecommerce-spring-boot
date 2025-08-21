package kr.hhplus.be.server.monitoring;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 캐시 성능 메트릭 수집기
 * - 캐시 히트율 추적
 * - Redis 상태 모니터링
 * - 성능 지표 제공
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheMetricsCollector {

    @Qualifier("cacheRedisTemplate")
    private final RedisTemplate<String, Object> cacheRedisTemplate;

    private final CacheManager cacheManager;

    /**
     * KEYS 명령어 대신 SCAN 사용으로 성능 개선
     */
    public Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();

        try (var cursor = cacheRedisTemplate.scan(options)) {
            cursor.forEachRemaining(keys::add);
        }

        return keys;
    }

    /**
     * 캐시 히트 기록
     */
    public void recordCacheHit(String cacheName) {
        String metricKey = "ecommerce:metrics:cache:hit:" + LocalDate.now();
        cacheRedisTemplate.opsForHash().increment(metricKey, cacheName, 1);
        cacheRedisTemplate.expire(metricKey, Duration.ofDays(7));
        log.debug("캐시 히트 기록: {}", cacheName);
    }

    /**
     * 캐시 미스 기록
     */
    public void recordCacheMiss(String cacheName) {
        String metricKey = "ecommerce:metrics:cache:miss:" + LocalDate.now();
        cacheRedisTemplate.opsForHash().increment(metricKey, cacheName, 1);
        cacheRedisTemplate.expire(metricKey, Duration.ofDays(7));
        log.debug("캐시 미스 기록: {}", cacheName);
    }

    /**
     * 캐시 메트릭 조회
     */
    public Map<String, Object> getCacheMetrics() {
        LocalDate today = LocalDate.now();
        String hitKey = "ecommerce:metrics:cache:hit:" + today;
        String missKey = "ecommerce:metrics:cache:miss:" + today;

        Map<Object, Object> hits = cacheRedisTemplate.opsForHash().entries(hitKey);
        Map<Object, Object> misses = cacheRedisTemplate.opsForHash().entries(missKey);

        Map<String, Double> hitRates = new HashMap<>();
        for (Object cache : hits.keySet()) {
            long hitCount = Long.parseLong(hits.get(cache).toString());
            long missCount = Long.parseLong(misses.getOrDefault(cache, "0").toString());

            if (hitCount + missCount > 0) {
                double hitRate = (double) hitCount / (hitCount + missCount) * 100;
                hitRates.put(cache.toString(), hitRate);
            }
        }

        return Map.of(
                "hitRates", hitRates,
                "totalHits", hits.values().stream().mapToLong(v -> Long.parseLong(v.toString())).sum(),
                "totalMisses", misses.values().stream().mapToLong(v -> Long.parseLong(v.toString())).sum(),
                "date", today.toString());
    }

    /**
     * Redis 상태 확인
     */
    public Map<String, Object> getRedisStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            // Redis 연결 상태 확인
            cacheRedisTemplate.opsForValue().set("health:check", "ok", Duration.ofSeconds(1));
            String result = (String) cacheRedisTemplate.opsForValue().get("health:check");
            status.put("connected", "ok".equals(result));

            // 활성 키 개수 (SCAN 사용으로 성능 개선)
            Set<String> productKeys = scanKeys("ecommerce::products::*");
            Set<String> popularKeys = scanKeys("ecommerce::popular-products::*");
            Set<String> lockKeys = scanKeys("ecommerce:lock:*");

            status.put("productCacheCount", productKeys.size());
            status.put("popularCacheCount", popularKeys.size());
            status.put("activeLockCount", lockKeys.size());

        } catch (Exception e) {
            status.put("connected", false);
            status.put("error", e.getMessage());
            log.error("Redis 상태 확인 실패", e);
        }

        return status;
    }
}

/**
 * 캐시 및 분산락 모니터링 API
 */
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@Slf4j
class CacheMonitoringController {

    private final CacheMetricsCollector metricsCollector;

    @Qualifier("cacheRedisTemplate")
    private final RedisTemplate<String, Object> cacheRedisTemplate;

    @GetMapping("/cache/metrics")
    public Map<String, Object> getCacheMetrics() {
        return metricsCollector.getCacheMetrics();
    }

    @GetMapping("/redis/status")
    public Map<String, Object> getRedisStatus() {
        return metricsCollector.getRedisStatus();
    }

    @GetMapping("/locks/status")
    public Map<String, Object> getLockStatus() {
        Set<String> lockKeys = metricsCollector.scanKeys("ecommerce:lock:*");

        Map<String, Object> lockStatus = new HashMap<>();
        if (lockKeys != null) {
            for (String key : lockKeys) {
                String value = (String) cacheRedisTemplate.opsForValue().get(key);
                Long ttl = cacheRedisTemplate.getExpire(key);

                lockStatus.put(key, Map.of(
                        "value", value != null ? value : "null",
                        "ttl", ttl,
                        "isLocked", value != null));
            }
        }

        return Map.of(
                "activeLocks", lockStatus.size(),
                "locks", lockStatus,
                "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/performance/summary")
    public Map<String, Object> getPerformanceSummary() {
        Map<String, Object> summary = new HashMap<>();

        // 캐시 메트릭
        Map<String, Object> cacheMetrics = metricsCollector.getCacheMetrics();
        summary.put("cache", cacheMetrics);

        // Redis 상태
        Map<String, Object> redisStatus = metricsCollector.getRedisStatus();
        summary.put("redis", redisStatus);

        // 활성 락 개수 (SCAN 사용)
        Set<String> lockKeys = metricsCollector.scanKeys("ecommerce:lock:*");
        summary.put("activeLocks", lockKeys.size());

        return summary;
    }
}