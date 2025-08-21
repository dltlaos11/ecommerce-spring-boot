package kr.hhplus.be.server.ranking.integration;

import kr.hhplus.be.server.order.event.OrderCompletedEvent;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.repository.ProductRepository;
import kr.hhplus.be.server.ranking.dto.ProductRankingResponse;
import kr.hhplus.be.server.ranking.service.ProductRankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 랭킹 시스템 통합 테스트
 * 
 * Redis TestContainer와 함께 실제 Redis 동작을 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Transactional
class RankingIntegrationTest {

    @Autowired
    private ProductRankingService rankingService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Product product1;
    private Product product2;
    private Product product3;

    @BeforeEach
    void setUp() {
        // Redis 데이터 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // 테스트용 상품 데이터 생성
        product1 = new Product("인기상품1", new BigDecimal("10000"), 100);
        product2 = new Product("인기상품2", new BigDecimal("20000"), 50);
        product3 = new Product("인기상품3", new BigDecimal("30000"), 30);

        productRepository.save(product1);
        productRepository.save(product2);
        productRepository.save(product3);
    }

    @Test
    @DisplayName("주문 완료 이벤트가 발생하면 Redis에 랭킹 데이터가 축적된다")
    void orderCompletedEvent_ShouldAccumulateRankingData() {
        // Given: 상품1에 대한 여러 주문 완료 이벤트
        OrderCompletedEvent event1 = new OrderCompletedEvent(
                1L, 100L, product1.getId(), product1.getName(), 5, LocalDateTime.now()
        );
        OrderCompletedEvent event2 = new OrderCompletedEvent(
                2L, 101L, product1.getId(), product1.getName(), 3, LocalDateTime.now()
        );
        OrderCompletedEvent event3 = new OrderCompletedEvent(
                3L, 102L, product2.getId(), product2.getName(), 7, LocalDateTime.now()
        );

        // When: 이벤트 처리
        rankingService.handleOrderCompleted(event1);
        rankingService.handleOrderCompleted(event2);
        rankingService.handleOrderCompleted(event3);

        // Then: 랭킹 데이터 확인
        List<ProductRankingResponse> rankings = rankingService.getDailyTopProducts(10);

        assertThat(rankings).hasSize(2);
        
        // 상품1이 1위 (5+3=8개 주문)
        ProductRankingResponse first = rankings.get(0);
        assertThat(first.rank()).isEqualTo(1);
        assertThat(first.productId()).isEqualTo(product1.getId());
        assertThat(first.orderCount()).isEqualTo(8L);

        // 상품2가 2위 (7개 주문)
        ProductRankingResponse second = rankings.get(1);
        assertThat(second.rank()).isEqualTo(2);
        assertThat(second.productId()).isEqualTo(product2.getId());
        assertThat(second.orderCount()).isEqualTo(7L);
    }

    @Test
    @DisplayName("일간 랭킹과 3일 집계 랭킹이 정상 동작한다")
    void dailyAndWeeklyRanking_ShouldWorkCorrectly() {
        // Given: 각 상품에 대한 주문 이벤트 생성
        // 상품1: 10개 주문
        for (int i = 0; i < 10; i++) {
            OrderCompletedEvent event = new OrderCompletedEvent(
                    (long) i, 100L, product1.getId(), product1.getName(), 1, LocalDateTime.now()
            );
            rankingService.handleOrderCompleted(event);
        }

        // 상품2: 15개 주문
        for (int i = 10; i < 25; i++) {
            OrderCompletedEvent event = new OrderCompletedEvent(
                    (long) i, 101L, product2.getId(), product2.getName(), 1, LocalDateTime.now()
            );
            rankingService.handleOrderCompleted(event);
        }

        // 상품3: 5개 주문
        for (int i = 25; i < 30; i++) {
            OrderCompletedEvent event = new OrderCompletedEvent(
                    (long) i, 102L, product3.getId(), product3.getName(), 1, LocalDateTime.now()
            );
            rankingService.handleOrderCompleted(event);
        }

        // When & Then: 일간 랭킹 조회
        List<ProductRankingResponse> dailyRankings = rankingService.getDailyTopProducts(5);
        
        assertThat(dailyRankings).hasSize(3);
        assertThat(dailyRankings.get(0).productId()).isEqualTo(product2.getId()); // 15개
        assertThat(dailyRankings.get(1).productId()).isEqualTo(product1.getId()); // 10개
        assertThat(dailyRankings.get(2).productId()).isEqualTo(product3.getId()); // 5개

        // When & Then: 3일 집계 랭킹 조회 (현재는 당일 데이터만 있음)
        List<ProductRankingResponse> weeklyRankings = rankingService.getWeeklyTopProducts(5);
        
        assertThat(weeklyRankings).hasSize(3);
        assertThat(weeklyRankings.get(0).productId()).isEqualTo(product2.getId());
        assertThat(weeklyRankings.get(1).productId()).isEqualTo(product1.getId());
        assertThat(weeklyRankings.get(2).productId()).isEqualTo(product3.getId());
    }

    @Test
    @DisplayName("특정 상품의 랭킹을 정확히 조회할 수 있다")
    void getProductRank_ShouldReturnCorrectRank() {
        // Given: 랭킹 데이터 설정
        OrderCompletedEvent event1 = new OrderCompletedEvent(
                1L, 100L, product1.getId(), product1.getName(), 20, LocalDateTime.now()
        );
        OrderCompletedEvent event2 = new OrderCompletedEvent(
                2L, 101L, product2.getId(), product2.getName(), 30, LocalDateTime.now()
        );
        OrderCompletedEvent event3 = new OrderCompletedEvent(
                3L, 102L, product3.getId(), product3.getName(), 10, LocalDateTime.now()
        );

        rankingService.handleOrderCompleted(event1);
        rankingService.handleOrderCompleted(event2);
        rankingService.handleOrderCompleted(event3);

        // When & Then: 각 상품의 랭킹 확인
        Long product1Rank = rankingService.getProductRank(product1.getId());
        Long product2Rank = rankingService.getProductRank(product2.getId());
        Long product3Rank = rankingService.getProductRank(product3.getId());

        assertThat(product2Rank).isEqualTo(1L); // 30개로 1위
        assertThat(product1Rank).isEqualTo(2L); // 20개로 2위
        assertThat(product3Rank).isEqualTo(3L); // 10개로 3위
    }

    @Test
    @DisplayName("랭킹 데이터 초기화가 정상 동작한다")
    void clearRankingData_ShouldWork() {
        // Given: 랭킹 데이터 생성
        OrderCompletedEvent event = new OrderCompletedEvent(
                1L, 100L, product1.getId(), product1.getName(), 5, LocalDateTime.now()
        );
        rankingService.handleOrderCompleted(event);

        // 데이터가 있는지 확인
        List<ProductRankingResponse> beforeClear = rankingService.getDailyTopProducts(10);
        assertThat(beforeClear).hasSize(1);

        // When: 랭킹 데이터 초기화
        rankingService.clearRankingData(LocalDate.now());

        // Then: 데이터가 삭제되었는지 확인
        List<ProductRankingResponse> afterClear = rankingService.getDailyTopProducts(10);
        assertThat(afterClear).isEmpty();
    }

    @Test
    @DisplayName("Redis 키 TTL이 올바르게 설정된다")
    void redisKeyTtl_ShouldBeSetCorrectly() {
        // Given & When: 랭킹 이벤트 처리
        OrderCompletedEvent event = new OrderCompletedEvent(
                1L, 100L, product1.getId(), product1.getName(), 1, LocalDateTime.now()
        );
        rankingService.handleOrderCompleted(event);

        // Then: TTL 확인
        String dailyKey = "ranking:product:daily:" + LocalDate.now();
        Long ttl = redisTemplate.getExpire(dailyKey);
        
        // TTL이 설정되어 있고, 5일 이내인지 확인
        assertThat(ttl).isGreaterThan(0);
        assertThat(ttl).isLessThanOrEqualTo(5 * 24 * 60 * 60); // 5일 = 432000초
    }
}