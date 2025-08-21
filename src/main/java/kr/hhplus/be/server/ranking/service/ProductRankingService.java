package kr.hhplus.be.server.ranking.service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import kr.hhplus.be.server.order.event.OrderCompletedEvent;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.product.service.ProductService;
import kr.hhplus.be.server.ranking.dto.ProductRankingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 기반 상품 랭킹 서비스
 * 
 * - @TransactionalEventListener 활용
 * - Redis Sorted Set 최적화
 * - 메타데이터 분리: 랭킹(Redis) + 상품정보(DB)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRankingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductService productService;

    // Redis 키 패턴
    private static final String DAILY_RANKING_KEY_PREFIX = "ranking:product:daily:";
    private static final String WEEKLY_RANKING_KEY_PREFIX = "ranking:product:3days:";

    /**
     * 주문 완료 이벤트 처리 - 랭킹 메트릭 수집
     * 
     * @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
     *                                   - 트랜잭션 커밋 후에만 실행되어 데이터 일관성 보장
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        try {
            log.info("📊 랭킹 메트릭 수집: productId={}, quantity={}",
                    event.getProductId(), event.getQuantity());

            updateDailyRanking(event.getProductId(), event.getQuantity());

        } catch (Exception e) {
            log.error("❌ 랭킹 메트릭 수집 실패: productId={}", event.getProductId(), e);
            // 랭킹 업데이트 실패는 비즈니스 로직에 영향주지 않음
        }
    }

    /**
     * 일간 랭킹 업데이트
     */
    private void updateDailyRanking(Long productId, Integer quantity) {
        String dailyKey = DAILY_RANKING_KEY_PREFIX + LocalDate.now();

        // Redis Sorted Set에 상품별 주문 수량 누적
        redisTemplate.opsForZSet().incrementScore(dailyKey, productId.toString(), quantity);

        // TTL 설정: 5일 후 자동 만료 (여유롭게 설정)
        redisTemplate.expire(dailyKey, Duration.ofDays(5));

        log.debug("📈 일간 랭킹 업데이트: key={}, productId={}, quantity={}",
                dailyKey, productId, quantity);
    }

    /**
     * 일간 TOP 랭킹 조회
     */
    public List<ProductRankingResponse> getDailyTopProducts(int limit) {
        String dailyKey = DAILY_RANKING_KEY_PREFIX + LocalDate.now();
        return getTopProductsFromKey(dailyKey, limit);
    }

    /**
     * 3일 집계 랭킹 조회 (ZUNIONSTORE 활용)
     */
    public List<ProductRankingResponse> getWeeklyTopProducts(int limit) {
        LocalDate today = LocalDate.now();
        String weeklyKey = WEEKLY_RANKING_KEY_PREFIX + today;

        // 3일간의 키 생성
        String day1Key = DAILY_RANKING_KEY_PREFIX + today;
        String day2Key = DAILY_RANKING_KEY_PREFIX + today.minusDays(1);
        String day3Key = DAILY_RANKING_KEY_PREFIX + today.minusDays(2);

        try {
            // ZUNIONSTORE로 3일 데이터 합산
            redisTemplate.opsForZSet().unionAndStore(day1Key,
                    List.of(day2Key, day3Key), weeklyKey);

            // 3일 집계 데이터 TTL 설정
            redisTemplate.expire(weeklyKey, Duration.ofDays(1));

            return getTopProductsFromKey(weeklyKey, limit);

        } catch (Exception e) {
            log.error("❌ 3일 집계 랭킹 조회 실패", e);
            // 실패 시 당일 랭킹으로 대체
            return getDailyTopProducts(limit);
        }
    }

    /**
     * Redis 키에서 TOP 상품 조회 후 DB 메타데이터와 결합
     * 
     * 메타데이터 분리 전략:
     * - 랭킹 계산 (Redis): 빠른 성능
     * - 상품 정보 (DB): 정확한 메타데이터
     */
    private List<ProductRankingResponse> getTopProductsFromKey(String key, int limit) {
        // 1. Redis에서 랭킹 데이터 조회 (점수 포함)
        Set<ZSetOperations.TypedTuple<Object>> rankings = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0,
                limit - 1);

        if (rankings.isEmpty()) {
            log.info("📊 랭킹 데이터 없음: key={}", key);
            return List.of();
        }

        // 2. 상품 ID 목록 추출
        List<Long> productIds = rankings.stream()
                .map(tuple -> Long.parseLong(tuple.getValue().toString()))
                .collect(Collectors.toList());

        // 3. DB에서 상품 정보 일괄 조회 (기존 서비스 재사용)
        List<ProductResponse> products = productService.getProductsByIds(productIds);

        // 4. 랭킹 데이터와 상품 정보 결합
        AtomicInteger rank = new AtomicInteger(1);

        return rankings.stream()
                .map(tuple -> {
                    Long productId = Long.parseLong(tuple.getValue().toString());
                    Double score = tuple.getScore();

                    ProductResponse product = products.stream()
                            .filter(p -> p.id().equals(productId))
                            .findFirst()
                            .orElse(null);

                    if (product == null) {
                        log.warn("⚠️ 상품 정보 없음: productId={}", productId);
                        return null;
                    }

                    return ProductRankingResponse.of(
                            rank.getAndIncrement(),
                            product.id(),
                            product.name(),
                            product.price(),
                            product.stockQuantity(),
                            score.longValue(), // 주문 수량 합계
                            score);
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }

    /**
     * 특정 상품의 현재 랭킹 조회
     */
    public Long getProductRank(Long productId) {
        String dailyKey = DAILY_RANKING_KEY_PREFIX + LocalDate.now();
        Long rank = redisTemplate.opsForZSet().reverseRank(dailyKey, productId.toString());
        return rank != null ? rank + 1 : null; // 1부터 시작하는 순위
    }

    /**
     * 랭킹 데이터 초기화 (관리자 기능)
     */
    public void clearRankingData(LocalDate date) {
        String dailyKey = DAILY_RANKING_KEY_PREFIX + date;
        redisTemplate.delete(dailyKey);
        log.info("🗑️ 랭킹 데이터 초기화: date={}", date);
    }
}