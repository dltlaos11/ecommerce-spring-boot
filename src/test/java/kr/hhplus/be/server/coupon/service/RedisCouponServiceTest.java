package kr.hhplus.be.server.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.dto.AsyncCouponIssueRequest;
import kr.hhplus.be.server.coupon.dto.AsyncCouponIssueResponse;
import kr.hhplus.be.server.coupon.exception.CouponAlreadyIssuedException;
import kr.hhplus.be.server.coupon.exception.CouponExhaustedException;
import kr.hhplus.be.server.coupon.repository.CouponRepository;

/**
 * RedisCouponService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class RedisCouponServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private RedisCouponService redisCouponService;

    @Test
    @DisplayName("비동기 쿠폰 발급 요청이 성공적으로 처리된다")
    void requestCouponIssueAsync_ShouldSuccess() throws Exception {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        Long userId = 1L;
        Long couponId = 100L;
        AsyncCouponIssueRequest request = new AsyncCouponIssueRequest(userId, couponId);

        // Redis 검증 Mock
        when(setOperations.isMember(anyString(), anyString())).thenReturn(false); // 중복 발급 아님
        when(valueOperations.get(anyString())).thenReturn("10"); // 재고 10개
        when(valueOperations.decrement(anyString())).thenReturn(9L); // 재고 차감 성공

        // ObjectMapper Mock
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        AsyncCouponIssueResponse response = redisCouponService.requestCouponIssueAsync(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.requestId()).isNotNull();
        assertThat(response.message()).contains("대기열에 추가");

        // Redis 연산 검증
        verify(setOperations).isMember(contains("coupon:issued:"), eq("1"));
        verify(valueOperations).decrement(contains("coupon:stock:"));
        verify(setOperations).add(contains("coupon:issued:"), eq("1"));
        verify(zSetOperations).add(eq("coupon:queue:processing"), anyString(), anyDouble());
    }

    @Test
    @DisplayName("중복 발급 시 예외가 발생한다")
    void requestCouponIssueAsync_ShouldThrowException_WhenAlreadyIssued() {
        // Given
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations); // saveRequestStatus에서 필요

        Long userId = 1L;
        Long couponId = 100L;
        AsyncCouponIssueRequest request = new AsyncCouponIssueRequest(userId, couponId);

        when(setOperations.isMember(anyString(), anyString())).thenReturn(true); // 이미 발급됨

        // When & Then
        assertThatThrownBy(() -> redisCouponService.requestCouponIssueAsync(request))
                .isInstanceOf(CouponAlreadyIssuedException.class);

        // 이후 처리가 진행되지 않았는지 확인
        verify(valueOperations, never()).decrement(anyString());
        verify(zSetOperations, never()).add(anyString(), anyString(), anyDouble());
    }

    @Test
    @DisplayName("재고 부족 시 예외가 발생한다")
    void requestCouponIssueAsync_ShouldThrowException_WhenStockExhausted() {
        // Given
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Long userId = 1L;
        Long couponId = 100L;
        AsyncCouponIssueRequest request = new AsyncCouponIssueRequest(userId, couponId);

        when(setOperations.isMember(anyString(), anyString())).thenReturn(false);
        when(valueOperations.get(anyString())).thenReturn("0"); // 재고 0개

        // When & Then
        assertThatThrownBy(() -> redisCouponService.requestCouponIssueAsync(request))
                .isInstanceOf(CouponExhaustedException.class);

        verify(valueOperations, never()).decrement(anyString());
    }

    @Test
    @DisplayName("재고 차감 실패 시 예외가 발생한다")
    void requestCouponIssueAsync_ShouldThrowException_WhenDecrementFails() {
        // Given
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Long userId = 1L;
        Long couponId = 100L;
        AsyncCouponIssueRequest request = new AsyncCouponIssueRequest(userId, couponId);

        when(setOperations.isMember(anyString(), anyString())).thenReturn(false);
        when(valueOperations.get(anyString())).thenReturn("5");
        when(valueOperations.decrement(anyString())).thenReturn(-1L); // 재고 차감 후 음수

        // When & Then
        assertThatThrownBy(() -> redisCouponService.requestCouponIssueAsync(request))
                .isInstanceOf(CouponExhaustedException.class);

        // 롤백 처리 확인 (increment 호출이 2번: decrementStock과 rollbackRedisState)
        verify(valueOperations, atLeast(1)).increment(anyString());
    }

    @Test
    @DisplayName("요청 상태 조회가 정상 동작한다")
    void getRequestStatus_ShouldReturnCorrectStatus() throws Exception {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String requestId = "test-request-id";
        String statusJson = "{\"requestId\":\"test-request-id\",\"status\":\"COMPLETED\"}";
        AsyncCouponIssueResponse expectedResponse = new AsyncCouponIssueResponse(
                requestId, "COMPLETED", "완료", null, null, null);

        when(valueOperations.get("coupon:request:" + requestId)).thenReturn(statusJson);
        when(objectMapper.readValue(statusJson, AsyncCouponIssueResponse.class))
                .thenReturn(expectedResponse);

        // When
        AsyncCouponIssueResponse response = redisCouponService.getRequestStatus(requestId);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        verify(valueOperations).get("coupon:request:" + requestId);
        verify(objectMapper).readValue(statusJson, AsyncCouponIssueResponse.class);
    }

    @Test
    @DisplayName("존재하지 않는 요청 ID 조회 시 예외가 발생한다")
    void getRequestStatus_ShouldThrowException_WhenRequestNotFound() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String requestId = "non-existent-request";
        when(valueOperations.get("coupon:request:" + requestId)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> redisCouponService.getRequestStatus(requestId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 요청 ID");
    }

    @Test
    @DisplayName("쿠폰 재고 초기화가 정상 동작한다")
    void initializeCouponStock_ShouldInitializeCorrectly() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Long couponId = 100L;
        Coupon mockCoupon = createMockCoupon(couponId, 50); // 남은 재고 50개

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(mockCoupon));

        // When
        redisCouponService.initializeCouponStock(couponId);

        // Then
        verify(valueOperations).set(
                eq("coupon:stock:" + couponId),
                eq("50"),
                any());
    }

    @Test
    @DisplayName("큐 크기 조회가 정상 동작한다")
    void getQueueSize_ShouldReturnCorrectSize() {
        // Given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        Long expectedSize = 5L;
        when(zSetOperations.zCard("coupon:queue:processing")).thenReturn(expectedSize);

        // When
        Long queueSize = redisCouponService.getQueueSize();

        // Then
        assertThat(queueSize).isEqualTo(expectedSize);
        verify(zSetOperations).zCard("coupon:queue:processing");
    }

    @Test
    @DisplayName("현재 재고 조회가 정상 동작한다")
    void getCurrentStock_ShouldReturnCorrectStock() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Long couponId = 100L;
        String expectedStock = "25";
        when(valueOperations.get("coupon:stock:" + couponId)).thenReturn(expectedStock);

        // When
        Integer currentStock = redisCouponService.getCurrentStock(couponId);

        // Then
        assertThat(currentStock).isEqualTo(25);
        verify(valueOperations).get("coupon:stock:" + couponId);
    }

    @Test
    @DisplayName("재고 정보가 없는 경우 null을 반환한다")
    void getCurrentStock_ShouldReturnNull_WhenStockNotFound() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Long couponId = 100L;
        when(valueOperations.get("coupon:stock:" + couponId)).thenReturn(null);

        // When
        Integer currentStock = redisCouponService.getCurrentStock(couponId);

        // Then
        assertThat(currentStock).isNull();
    }

    private Coupon createMockCoupon(Long couponId, Integer remainingQuantity) {
        Coupon coupon = mock(Coupon.class);
        when(coupon.getRemainingQuantity()).thenReturn(remainingQuantity);
        return coupon;
    }
}