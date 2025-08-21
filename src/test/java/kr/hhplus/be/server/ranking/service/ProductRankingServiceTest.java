package kr.hhplus.be.server.ranking.service;

import kr.hhplus.be.server.order.event.OrderCompletedEvent;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.product.service.ProductService;
import kr.hhplus.be.server.ranking.dto.ProductRankingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProductRankingService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class ProductRankingServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ProductService productService;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private ProductRankingService rankingService;

    @BeforeEach
    void setUp() {
        // Mock 설정은 각 테스트에서 필요에 따라 설정
    }

    @Test
    @DisplayName("주문 완료 이벤트 처리 시 랭킹 메트릭이 수집된다")
    void handleOrderCompleted_ShouldUpdateRanking() {
        // Given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        
        OrderCompletedEvent event = new OrderCompletedEvent(
                1L, 100L, 1001L, "테스트 상품", 3, LocalDateTime.now()
        );
        
        String expectedKey = "ranking:product:daily:" + LocalDate.now();

        // When
        rankingService.handleOrderCompleted(event);

        // Then
        verify(zSetOperations).incrementScore(expectedKey, "1001", 3.0);
        verify(redisTemplate).expire(eq(expectedKey), any());
    }

    @Test
    @DisplayName("일간 TOP 랭킹 조회 시 상품 정보와 함께 반환된다")
    void getDailyTopProducts_ShouldReturnRankingWithProductInfo() {
        // Given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        
        int limit = 5;
        String dailyKey = "ranking:product:daily:" + LocalDate.now();
        
        // Redis에서 반환될 랭킹 데이터 Mock
        ZSetOperations.TypedTuple<Object> tuple1 = 
                ZSetOperations.TypedTuple.of("1001", 10.0);
        ZSetOperations.TypedTuple<Object> tuple2 = 
                ZSetOperations.TypedTuple.of("1002", 8.0);
        
        Set<ZSetOperations.TypedTuple<Object>> mockRankings = Set.of(tuple1, tuple2);
        
        when(zSetOperations.reverseRangeWithScores(dailyKey, 0, limit - 1))
                .thenReturn(mockRankings);

        // ProductService에서 반환될 상품 정보 Mock
        List<ProductResponse> mockProducts = List.of(
                new ProductResponse(1001L, "상품1", new BigDecimal("10000"), 100, LocalDateTime.now()),
                new ProductResponse(1002L, "상품2", new BigDecimal("20000"), 50, LocalDateTime.now())
        );
        
        when(productService.getProductsByIds(List.of(1001L, 1002L)))
                .thenReturn(mockProducts);

        // When
        List<ProductRankingResponse> result = rankingService.getDailyTopProducts(limit);

        // Then
        assertThat(result).hasSize(2);
        
        ProductRankingResponse first = result.get(0);
        assertThat(first.rank()).isEqualTo(1);
        assertThat(first.productId()).isEqualTo(1001L);
        assertThat(first.productName()).isEqualTo("상품1");
        assertThat(first.orderCount()).isEqualTo(10L);
        assertThat(first.score()).isEqualTo(10.0);

        ProductRankingResponse second = result.get(1);
        assertThat(second.rank()).isEqualTo(2);
        assertThat(second.productId()).isEqualTo(1002L);
        assertThat(second.productName()).isEqualTo("상품2");
        assertThat(second.orderCount()).isEqualTo(8L);
        assertThat(second.score()).isEqualTo(8.0);
    }

    @Test
    @DisplayName("3일 집계 랭킹 조회 시 ZUNIONSTORE가 실행된다")
    void getWeeklyTopProducts_ShouldUseZUnionStore() {
        // Given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        
        int limit = 3;
        LocalDate today = LocalDate.now();
        String weeklyKey = "ranking:product:3days:" + today;
        String day1Key = "ranking:product:daily:" + today;
        String day2Key = "ranking:product:daily:" + today.minusDays(1);
        String day3Key = "ranking:product:daily:" + today.minusDays(2);

        // ZUNIONSTORE 결과 Mock
        ZSetOperations.TypedTuple<Object> tuple = 
                ZSetOperations.TypedTuple.of("1001", 15.0);
        Set<ZSetOperations.TypedTuple<Object>> mockResult = Set.of(tuple);
        
        when(zSetOperations.reverseRangeWithScores(weeklyKey, 0, limit - 1))
                .thenReturn(mockResult);

        // 상품 정보 Mock
        List<ProductResponse> mockProducts = List.of(
                new ProductResponse(1001L, "상품1", new BigDecimal("10000"), 100, LocalDateTime.now())
        );
        when(productService.getProductsByIds(List.of(1001L)))
                .thenReturn(mockProducts);

        // When
        List<ProductRankingResponse> result = rankingService.getWeeklyTopProducts(limit);

        // Then
        verify(zSetOperations).unionAndStore(eq(day1Key), eq(List.of(day2Key, day3Key)), eq(weeklyKey));
        verify(redisTemplate).expire(eq(weeklyKey), any());
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).productId()).isEqualTo(1001L);
        assertThat(result.get(0).score()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("특정 상품의 랭킹 조회가 정상 동작한다")
    void getProductRank_ShouldReturnCorrectRank() {
        // Given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        
        Long productId = 1001L;
        String dailyKey = "ranking:product:daily:" + LocalDate.now();
        
        when(zSetOperations.reverseRank(dailyKey, productId.toString()))
                .thenReturn(2L); // 0-based index에서 2는 3위

        // When
        Long rank = rankingService.getProductRank(productId);

        // Then
        assertThat(rank).isEqualTo(3L); // 1-based index로 변환됨
    }

    @Test
    @DisplayName("랭킹 데이터 초기화가 정상 동작한다")
    void clearRankingData_ShouldDeleteRedisKey() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 1);
        String expectedKey = "ranking:product:daily:" + date;

        // When
        rankingService.clearRankingData(date);

        // Then
        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    @DisplayName("랭킹 데이터가 없는 경우 빈 리스트를 반환한다")
    void getDailyTopProducts_WhenNoData_ShouldReturnEmptyList() {
        // Given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        
        int limit = 10;
        String dailyKey = "ranking:product:daily:" + LocalDate.now();
        
        when(zSetOperations.reverseRangeWithScores(dailyKey, 0, limit - 1))
                .thenReturn(Set.of());

        // When
        List<ProductRankingResponse> result = rankingService.getDailyTopProducts(limit);

        // Then
        assertThat(result).isEmpty();
        verify(productService, never()).getProductsByIds(any());
    }
}