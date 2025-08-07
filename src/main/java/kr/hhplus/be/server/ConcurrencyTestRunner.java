package kr.hhplus.be.server;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import kr.hhplus.be.server.balance.dto.BalanceResponse;
import kr.hhplus.be.server.balance.service.BalanceService;
import kr.hhplus.be.server.coupon.dto.AvailableCouponResponse;
import kr.hhplus.be.server.coupon.dto.IssuedCouponResponse;
import kr.hhplus.be.server.coupon.service.CouponService;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 동시성 테스트 실행 러너
 * 
 * 실행방법:
 * ./gradlew bootRun --args='--spring.profiles.active=concurrency-test'
 */
@Slf4j
@Component
@Profile("concurrency-test")
@RequiredArgsConstructor
public class ConcurrencyTestRunner implements CommandLineRunner {

    private final BalanceService balanceService;
    private final ProductService productService;
    private final CouponService couponService;

    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 동시성 테스트 시작!");

        // 3초 대기 (초기화 시간 - DataLoader 완료 대기)
        Thread.sleep(3000);

        // 테스트 데이터 생성 및 확인
        setupTestData();

        // 1. 낙관적 락 테스트 (잔액 충전)
        testOptimisticLockBalance();

        // 2. 비관적 락 테스트 (재고 차감)
        testPessimisticLockStock();

        // 3. 이중 방어 테스트 (쿠폰 발급)
        testCouponIssuance();

        log.info("🎉 모든 동시성 테스트 완료!");

        // 테스트 완료 후 애플리케이션 종료
        System.exit(0);
    }

    /**
     * 테스트 데이터 생성 및 확인
     */
    private void setupTestData() {
        log.info("\n=== 🔧 테스트 데이터 설정 ===");

        try {
            // 상품 목록 확인
            List<ProductResponse> products = productService.getAllProducts();
            log.info("사용 가능한 상품 수: {}개", products.size());

            if (products.isEmpty()) {
                log.warn("⚠️ 상품 데이터가 없습니다. 테스트용 상품을 생성합니다.");
                // 테스트용 상품 생성
                productService.createProduct("테스트 상품", new BigDecimal("10000"), 20);
                products = productService.getAllProducts();
            }

            // 쿠폰 목록 확인
            List<AvailableCouponResponse> coupons = couponService.getAvailableCoupons();
            log.info("사용 가능한 쿠폰 수: {}개", coupons.size());

            if (coupons.isEmpty()) {
                log.warn("⚠️ 쿠폰 데이터가 없습니다. 테스트용 쿠폰을 생성합니다.");
                // 테스트용 쿠폰 생성
                couponService.createCoupon(
                        "테스트 쿠폰",
                        kr.hhplus.be.server.coupon.domain.Coupon.DiscountType.FIXED,
                        new BigDecimal("5000"),
                        10, // 수량 10개
                        new BigDecimal("5000"),
                        new BigDecimal("10000"),
                        java.time.LocalDateTime.now().plusDays(30));
                coupons = couponService.getAvailableCoupons();
            }

            log.info("✅ 테스트 데이터 준비 완료");

        } catch (Exception e) {
            log.error("❌ 테스트 데이터 설정 실패: {}", e.getMessage());
            throw new RuntimeException("테스트 데이터 설정 실패", e);
        }
    }

    /**
     * 테스트 1: 낙관적 락 - 잔액 충전
     */
    private void testOptimisticLockBalance() throws Exception {
        log.info("\n=== 📊 테스트 1: 낙관적 락 (잔액 충전) ===");

        Long userId = 999L; // 테스트 전용 사용자
        BigDecimal chargeAmount = new BigDecimal("10000");
        int concurrentUsers = 10;

        log.info("🔧 설정: 사용자 {}, {}원씩 {}명 동시 충전", userId, chargeAmount, concurrentUsers);

        // 동시 충전 실행
        List<CompletableFuture<Void>> futures = IntStream.range(0, concurrentUsers)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        balanceService.chargeBalance(userId, chargeAmount);
                        log.debug("충전 성공: {}번째", i + 1);
                    } catch (Exception e) {
                        log.warn("충전 실패: {}번째 - {}", i + 1, e.getMessage());
                    }
                }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 결과 확인
        BalanceResponse result = balanceService.getUserBalance(userId);
        BigDecimal expected = chargeAmount.multiply(new BigDecimal(concurrentUsers));

        log.info("💰 결과:");
        log.info("  - 기대 잔액: {}원", expected);
        log.info("  - 실제 잔액: {}원", result.balance());
        log.info("  - 정합성: {}", expected.equals(result.balance()) ? "✅ 성공" : "❌ 실패");
    }

    /**
     * 테스트 2: 비관적 락 - 재고 차감
     */
    private void testPessimisticLockStock() throws Exception {
        log.info("\n=== 📦 테스트 2: 비관적 락 (재고 차감) ===");

        // 사용 가능한 첫 번째 상품 사용
        List<ProductResponse> products = productService.getAllProducts();
        if (products.isEmpty()) {
            log.error("❌ 테스트할 상품이 없습니다.");
            return;
        }

        ProductResponse testProduct = products.get(0);
        Long productId = testProduct.id();
        int initialStock = testProduct.stockQuantity();
        int concurrentOrders = Math.min(8, Math.max(5, initialStock / 2)); // 안전한 범위로 설정

        log.info("🔧 설정: 상품 {} ({}), 초기재고 {}개, 동시주문 {}건",
                productId, testProduct.name(), initialStock, concurrentOrders);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 동시 주문 실행
        List<CompletableFuture<Void>> futures = IntStream.range(0, concurrentOrders)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        productService.reduceStock(productId, 1);
                        int success = successCount.incrementAndGet();
                        log.debug("주문 성공: {}번째", success);
                    } catch (Exception e) {
                        int failure = failureCount.incrementAndGet();
                        log.debug("주문 실패: {}번째 - {}", failure, e.getClass().getSimpleName());
                    }
                }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 결과 확인
        ProductResponse finalProduct = productService.getProduct(productId);

        log.info("📊 결과:");
        log.info("  - 초기 재고: {}개", initialStock);
        log.info("  - 성공 주문: {}건", successCount.get());
        log.info("  - 실패 주문: {}건", failureCount.get());
        log.info("  - 최종 재고: {}개", finalProduct.stockQuantity());
        log.info("  - 정합성: {}",
                (initialStock - successCount.get() == finalProduct.stockQuantity()) ? "✅ 성공" : "❌ 실패");
    }

    /**
     * 테스트 3: 이중 방어 - 쿠폰 발급
     */
    private void testCouponIssuance() throws Exception {
        log.info("\n=== 🎫 테스트 3: 이중 방어 (쿠폰 발급) ===");

        // 사용 가능한 첫 번째 쿠폰 사용
        List<AvailableCouponResponse> coupons = couponService.getAvailableCoupons();
        if (coupons.isEmpty()) {
            log.error("❌ 테스트할 쿠폰이 없습니다.");
            return;
        }

        AvailableCouponResponse testCoupon = coupons.get(0);
        Long couponId = testCoupon.id();
        int availableQuantity = testCoupon.remainingQuantity();
        int concurrentRequests = Math.min(20, availableQuantity * 2); // 수량의 2배 요청

        log.info("🔧 설정: 쿠폰 {} ({}), 남은수량 {}개, 동시요청 {}건",
                couponId, testCoupon.name(), availableQuantity, concurrentRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);
        AtomicInteger exhaustedCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        // 동시 발급 요청
        List<CompletableFuture<Void>> futures = IntStream.range(0, concurrentRequests)
                .mapToObj(userId -> CompletableFuture.runAsync(() -> {
                    try {
                        IssuedCouponResponse response = couponService.issueCoupon(couponId, (long) userId + 5000);
                        int success = successCount.incrementAndGet();
                        log.debug("쿠폰 발급 성공: {}번째", success);
                    } catch (Exception e) {
                        String errorType = e.getClass().getSimpleName();
                        if (errorType.contains("AlreadyIssued")) {
                            duplicateCount.incrementAndGet();
                            log.debug("중복 발급 차단");
                        } else if (errorType.contains("Exhausted")) {
                            exhaustedCount.incrementAndGet();
                            log.debug("수량 소진");
                        } else {
                            otherErrorCount.incrementAndGet();
                            log.debug("기타 오류: {}", errorType);
                        }
                    }
                }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 결과 확인
        log.info("🎟️ 결과:");
        log.info("  - 발급 성공: {}건", successCount.get());
        log.info("  - 중복 차단: {}건", duplicateCount.get());
        log.info("  - 수량 소진: {}건", exhaustedCount.get());
        log.info("  - 기타 오류: {}건", otherErrorCount.get());
        log.info("  - 총 요청: {}건", concurrentRequests);
        log.info("  - 정합성: {}",
                (successCount.get() <= availableQuantity && successCount.get() > 0) ? "✅ 성공" : "❌ 실패");
    }
}