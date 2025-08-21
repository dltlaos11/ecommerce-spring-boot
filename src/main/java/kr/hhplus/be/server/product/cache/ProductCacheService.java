package kr.hhplus.be.server.product.cache;

import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 상품 캐시 서비스 - Cache-Aside Pattern 구현
 * TTL 랜덤화를 통한 Cache Stampede 방지
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCacheService {

    private final ProductRepository productRepository;
    
    @Qualifier("cacheRedisTemplate")
    private final RedisTemplate<String, Object> cacheRedisTemplate;
    
    private final Random random = new Random();

    /**
     * 상품 조회 - Spring Cache 어노테이션 활용
     */
    @Cacheable(value = "products", key = "#productId")
    public Optional<Product> findProductById(Long productId) {
        log.debug("캐시 미스 - DB에서 상품 조회: productId={}", productId);
        return productRepository.findById(productId);
    }

    /**
     * 상품 목록 조회 - 인기 상품 캐싱
     */
    @Cacheable(value = "popular-products", key = "'top10'", unless = "#result == null || #result.isEmpty()")
    public List<Product> getPopularProducts() {
        log.debug("캐시 미스 - DB에서 인기 상품 조회");
        // 실제로는 조회수나 판매량 기준으로 정렬해야 하지만, 여기서는 단순히 전체 조회
        return productRepository.findAll().stream().limit(10).toList();
    }

    /**
     * 수동 캐시 설정 - TTL 랜덤화 적용
     */
    public void cacheProductManually(Long productId, Product product) {
        String key = generateCacheKey("product", productId.toString());
        
        // TTL 랜덤화: 기본 30분 + 0~10분 랜덤
        Duration baseTtl = Duration.ofMinutes(30);
        long randomSeconds = random.nextInt(600); // 0~10분
        Duration finalTtl = baseTtl.plusSeconds(randomSeconds);
        
        cacheRedisTemplate.opsForValue().set(key, product, finalTtl);
        log.debug("상품 수동 캐시 설정: productId={}, ttl={}초", productId, finalTtl.getSeconds());
    }

    /**
     * 상품 업데이트 시 캐시 무효화
     */
    @CacheEvict(value = {"products", "popular-products"}, key = "#productId", allEntries = false)
    public void evictProductCache(Long productId) {
        log.debug("상품 캐시 무효화: productId={}", productId);
        
        // 관련 캐시들도 함께 무효화
        cacheRedisTemplate.delete(generateCacheKey("product", productId.toString()));
        cacheRedisTemplate.delete("ecommerce::popular-products::top10");
    }

    /**
     * 전체 상품 캐시 무효화 (대량 업데이트 시)
     */
    @CacheEvict(value = {"products", "popular-products"}, allEntries = true)
    public void evictAllProductCache() {
        log.info("전체 상품 캐시 무효화");
    }

    /**
     * Cache Stampede 방지를 위한 분산락 적용 조회
     */
    public Optional<Product> findProductByIdWithStampedeProtection(Long productId) {
        String cacheKey = generateCacheKey("product", productId.toString());
        String lockKey = "lock:product:" + productId;

        Product cached = (Product) cacheRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("캐시 히트: productId={}", productId);
            return Optional.of(cached);
        }

        // 분산락으로 한 번에 하나의 요청만 DB 조회
        Boolean lockAcquired = cacheRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(5));

        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                // Double-check: 다른 스레드가 이미 캐시했을 수 있음
                cached = (Product) cacheRedisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    return Optional.of(cached);
                }

                // DB 조회 및 캐시 설정
                Optional<Product> product = productRepository.findById(productId);
                if (product.isPresent()) {
                    cacheProductManually(productId, product.get());
                }
                return product;
            } finally {
                cacheRedisTemplate.delete(lockKey);
            }
        } else {
            // 락 획득 실패 시 백오프 후 재시도
            return retryWithBackoff(productId, 3);
        }
    }

    private Optional<Product> retryWithBackoff(Long productId, int maxRetries) {
        String cacheKey = generateCacheKey("product", productId.toString());
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                Thread.sleep(100 * (i + 1)); // 백오프: 100ms, 200ms, 300ms
                Product cached = (Product) cacheRedisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    return Optional.of(cached);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // 최종적으로 DB에서 직접 조회 (캐시 없이)
        log.warn("캐시 스탬피드 백오프 실패, DB 직접 조회: productId={}", productId);
        return productRepository.findById(productId);
    }

    private String generateCacheKey(String prefix, String suffix) {
        return "ecommerce::" + prefix + "::" + suffix;
    }
}