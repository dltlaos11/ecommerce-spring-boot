package kr.hhplus.be.server.concurrency;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import kr.hhplus.be.server.balance.dto.BalanceResponse;
import kr.hhplus.be.server.balance.service.BalanceService;
import kr.hhplus.be.server.common.test.IntegrationTestBase;
import kr.hhplus.be.server.coupon.dto.AvailableCouponResponse;
import kr.hhplus.be.server.coupon.service.CouponService;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.product.service.ProductService;

import static org.assertj.core.api.Assertions.assertThat;
import lombok.extern.slf4j.Slf4j;

/**
 * 동시성 제어 통합 테스트 - FIRST 원칙 준수
 * Fast, Independent, Repeatable, Self-Validating, Timely
 */
@Slf4j
@DisplayName("동시성 제어 통합 테스트")
class ConcurrencyIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CouponService couponService;

    @BeforeEach
    void setUp() {
        // 각 테스트가 독립적으로 실행되도록 데이터 준비
        setupTestData();
    }

    @Test
    @DisplayName("분산락을 통한 동시 잔액 충전 정합성 검증")
    void testDistributedLockBalance() throws Exception {
        // Given
        Long userId = 999L;
        BigDecimal chargeAmount = new BigDecimal("10000");
        int concurrentUsers = 10;
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When - 동시 충전 실행 (분산락으로 순차 처리)
        List<CompletableFuture<Void>> futures = IntStream.range(0, concurrentUsers)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        balanceService.chargeBalance(userId, chargeAmount);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        System.err.println("충전 실패: " + e.getMessage());
                    }
                }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Then - 분산락에서는 성공한 요청만큼 잔액이 증가해야 함
        BalanceResponse result = balanceService.getUserBalance(userId);
        BigDecimal expected = chargeAmount.multiply(new BigDecimal(successCount.get()));
        
        assertThat(result.balance()).isEqualByComparingTo(expected);
        assertThat(successCount.get()).isGreaterThan(0); // 적어도 일부는 성공해야 함
        assertThat(successCount.get() + failCount.get()).isEqualTo(concurrentUsers);
        
        log.info("분산락 테스트 결과 - 성공: {}, 실패: {}", successCount.get(), failCount.get());
    }

    @Test
    @DisplayName("비관적 락을 통한 동시 재고 차감 정합성 검증")
    void testPessimisticLockStock() throws Exception {
        // Given
        List<ProductResponse> products = productService.getAllProducts();
        ProductResponse testProduct = products.get(0);
        Long productId = testProduct.id();
        int initialStock = testProduct.stockQuantity();
        int concurrentOrders = Math.min(8, Math.max(5, initialStock / 2));

        AtomicInteger successCount = new AtomicInteger(0);

        // When - 동시 주문 실행
        List<CompletableFuture<Void>> futures = IntStream.range(0, concurrentOrders)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        productService.reduceStock(productId, 1);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 재고 부족은 예상된 상황
                    }
                }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Then
        ProductResponse finalProduct = productService.getProduct(productId);
        assertThat(initialStock - successCount.get()).isEqualTo(finalProduct.stockQuantity());
    }

    @Test
    @DisplayName("이중 방어 전략을 통한 동시 쿠폰 발급 정합성 검증")
    void testDoubleDefenseIssuance() throws Exception {
        // Given
        List<AvailableCouponResponse> coupons = couponService.getAvailableCoupons();
        AvailableCouponResponse testCoupon = coupons.get(0);
        Long couponId = testCoupon.id();
        int availableQuantity = testCoupon.remainingQuantity();
        int concurrentRequests = Math.min(20, availableQuantity * 2);

        AtomicInteger successCount = new AtomicInteger(0);

        // When - 동시 발급 요청
        List<CompletableFuture<Void>> futures = IntStream.range(0, concurrentRequests)
                .mapToObj(userId -> CompletableFuture.runAsync(() -> {
                    try {
                        couponService.issueCoupon(couponId, (long) userId + 5000);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 중복 발급 차단은 예상된 상황
                    }
                }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Then - 발급된 쿠폰 수가 가용 수량을 초과하지 않음을 검증
        assertThat(successCount.get()).isLessThanOrEqualTo(availableQuantity);
        assertThat(successCount.get()).isGreaterThan(0);
    }

    private void setupTestData() {
        try {
            // 상품 데이터 확인 및 생성
            List<ProductResponse> products = productService.getAllProducts();
            if (products.isEmpty()) {
                productService.createProduct("동시성 테스트 상품", new BigDecimal("10000"), 20);
            }

            // 쿠폰 데이터 확인 및 생성
            List<AvailableCouponResponse> coupons = couponService.getAvailableCoupons();
            if (coupons.isEmpty()) {
                couponService.createCoupon(
                        "동시성 테스트 쿠폰",
                        kr.hhplus.be.server.coupon.domain.Coupon.DiscountType.FIXED,
                        new BigDecimal("5000"),
                        10,
                        new BigDecimal("5000"),
                        new BigDecimal("10000"),
                        java.time.LocalDateTime.now().plusDays(30));
            }
        } catch (Exception e) {
            throw new RuntimeException("테스트 데이터 설정 실패", e);
        }
    }
}