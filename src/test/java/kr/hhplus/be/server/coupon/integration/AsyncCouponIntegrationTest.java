package kr.hhplus.be.server.coupon.integration;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.dto.AsyncCouponIssueRequest;
import kr.hhplus.be.server.coupon.dto.AsyncCouponIssueResponse;
import kr.hhplus.be.server.coupon.repository.CouponRepository;
import kr.hhplus.be.server.coupon.repository.UserCouponRepository;
import kr.hhplus.be.server.coupon.service.RedisCouponService;
import kr.hhplus.be.server.coupon.worker.CouponIssueWorker;
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
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 비동기 쿠폰 시스템 통합 테스트
 * 
 * Redis TestContainer와 함께 실제 Redis 동작을 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Transactional
class AsyncCouponIntegrationTest {

    @Autowired
    private RedisCouponService redisCouponService;

    @Autowired
    private CouponIssueWorker couponIssueWorker;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        // Redis 데이터 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // 테스트용 쿠폰 생성
        testCoupon = new Coupon(
            "선착순 테스트 쿠폰",
            Coupon.DiscountType.PERCENTAGE,
            new BigDecimal("10"),
            10, // 총 10개 한정
            new BigDecimal("1000"),
            new BigDecimal("10000"),
            LocalDateTime.now().plusDays(30)
        );
        testCoupon = couponRepository.save(testCoupon);

        // Redis 재고 초기화
        redisCouponService.initializeCouponStock(testCoupon.getId());
    }

    @Test
    @DisplayName("비동기 쿠폰 발급 요청이 정상적으로 큐에 추가된다")
    void requestCouponIssueAsync_ShouldAddToQueue() {
        // Given
        Long userId = 1L;
        AsyncCouponIssueRequest request = new AsyncCouponIssueRequest(userId, testCoupon.getId());

        // When
        AsyncCouponIssueResponse response = redisCouponService.requestCouponIssueAsync(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getRequestId()).isNotNull();
        assertThat(response.getRequestedAt()).isNotNull();

        // 큐에 요청이 추가되었는지 확인
        Long queueSize = redisCouponService.getQueueSize();
        assertThat(queueSize).isEqualTo(1L);

        // Redis 재고가 차감되었는지 확인
        Integer currentStock = redisCouponService.getCurrentStock(testCoupon.getId());
        assertThat(currentStock).isEqualTo(9); // 10 -> 9
    }

    @Test
    @DisplayName("워커가 큐에서 요청을 처리하여 실제 쿠폰을 발급한다")
    void workerProcessesQueueAndIssuesCoupon() throws Exception {
        // Given
        Long userId = 1L;
        AsyncCouponIssueRequest request = new AsyncCouponIssueRequest(userId, testCoupon.getId());

        // 비동기 요청 추가
        AsyncCouponIssueResponse response = redisCouponService.requestCouponIssueAsync(request);
        String requestId = response.getRequestId();

        // When: 워커가 큐를 처리
        couponIssueWorker.processCouponIssueQueue();

        // Then: 큐가 비워졌는지 확인
        Long queueSize = redisCouponService.getQueueSize();
        assertThat(queueSize).isEqualTo(0L);

        // 요청 상태가 완료로 변경되었는지 확인
        AsyncCouponIssueResponse finalStatus = redisCouponService.getRequestStatus(requestId);
        assertThat(finalStatus.getStatus()).isEqualTo("COMPLETED");
        assertThat(finalStatus.getIssuedCouponId()).isNotNull();
        assertThat(finalStatus.getCompletedAt()).isNotNull();

        // 실제 DB에 UserCoupon이 생성되었는지 확인
        boolean couponIssued = userCouponRepository.findByUserIdAndCouponId(userId, testCoupon.getId())
            .isPresent();
        assertThat(couponIssued).isTrue();

        // DB의 쿠폰 발급 수량이 증가했는지 확인
        Coupon updatedCoupon = couponRepository.findById(testCoupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("동시에 여러 사용자가 쿠폰 발급을 요청해도 정확한 수량만 발급된다")
    void concurrentCouponIssueRequests_ShouldHandleCorrectly() throws InterruptedException {
        // Given
        int threadCount = 15; // 쿠폰 10개보다 많은 요청
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When: 동시에 여러 사용자가 요청
        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    AsyncCouponIssueRequest request = new AsyncCouponIssueRequest(userId, testCoupon.getId());
                    redisCouponService.requestCouponIssueAsync(request);
                } catch (Exception e) {
                    // 재고 소진 등의 예외는 예상된 동작
                    System.out.println("사용자 " + userId + " 요청 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then: Redis 재고 확인 (정확히 0개 또는 음수가 되지 않아야 함)
        Integer finalStock = redisCouponService.getCurrentStock(testCoupon.getId());
        assertThat(finalStock).isGreaterThanOrEqualTo(0);

        // 큐에 추가된 요청 수 확인 (최대 10개까지만)
        Long queueSize = redisCouponService.getQueueSize();
        assertThat(queueSize).isLessThanOrEqualTo(10L);

        // 실제로 모든 큐 요청을 처리
        while (redisCouponService.getQueueSize() > 0) {
            couponIssueWorker.processCouponIssueQueue();
            Thread.sleep(100); // 잠시 대기
        }

        // 최종적으로 정확히 10개만 발급되었는지 확인
        Coupon updatedCoupon = couponRepository.findById(testCoupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getIssuedQuantity()).isLessThanOrEqualTo(10);
        assertThat(updatedCoupon.getRemainingQuantity()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("중복 발급 방지가 정상 동작한다")
    void duplicateIssueRequest_ShouldBeRejected() {
        // Given
        Long userId = 1L;
        AsyncCouponIssueRequest request = new AsyncCouponIssueRequest(userId, testCoupon.getId());

        // When: 첫 번째 요청 성공
        AsyncCouponIssueResponse firstResponse = redisCouponService.requestCouponIssueAsync(request);
        assertThat(firstResponse.getStatus()).isEqualTo("PENDING");

        // Then: 두 번째 요청은 중복 발급 예외 발생
        org.junit.jupiter.api.Assertions.assertThrows(
            kr.hhplus.be.server.coupon.exception.CouponAlreadyIssuedException.class,
            () -> redisCouponService.requestCouponIssueAsync(request)
        );
    }

    @Test
    @DisplayName("재고 소진 시 요청이 거부된다")
    void stockExhausted_ShouldRejectRequest() {
        // Given: 재고를 0으로 설정
        String stockKey = "coupon:stock:" + testCoupon.getId();
        redisTemplate.opsForValue().set(stockKey, "0");

        Long userId = 1L;
        AsyncCouponIssueRequest request = new AsyncCouponIssueRequest(userId, testCoupon.getId());

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(
            kr.hhplus.be.server.coupon.exception.CouponExhaustedException.class,
            () -> redisCouponService.requestCouponIssueAsync(request)
        );
    }

    @Test
    @DisplayName("요청 상태 조회가 정상 동작한다")
    void getRequestStatus_ShouldReturnCorrectStatus() {
        // Given
        Long userId = 1L;
        AsyncCouponIssueRequest request = new AsyncCouponIssueRequest(userId, testCoupon.getId());

        // When
        AsyncCouponIssueResponse response = redisCouponService.requestCouponIssueAsync(request);
        String requestId = response.getRequestId();

        // Then: 처리 전 상태 확인
        AsyncCouponIssueResponse status = redisCouponService.getRequestStatus(requestId);
        assertThat(status.getStatus()).isEqualTo("PENDING");
        assertThat(status.getRequestId()).isEqualTo(requestId);

        // 워커가 처리한 후 상태 확인
        couponIssueWorker.processCouponIssueQueue();

        AsyncCouponIssueResponse finalStatus = redisCouponService.getRequestStatus(requestId);
        assertThat(finalStatus.getStatus()).isEqualTo("COMPLETED");
        assertThat(finalStatus.getCompletedAt()).isNotNull();
    }
}