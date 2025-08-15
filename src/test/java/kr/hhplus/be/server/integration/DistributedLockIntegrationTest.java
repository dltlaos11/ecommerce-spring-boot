package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.balance.application.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.domain.UserBalance;
import kr.hhplus.be.server.balance.repository.UserBalanceRepository;
import kr.hhplus.be.server.config.TestcontainersConfiguration;
import kr.hhplus.be.server.coupon.application.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.repository.CouponRepository;
import kr.hhplus.be.server.order.application.CreateOrderUseCase;
import kr.hhplus.be.server.order.dto.CreateOrderRequest;
import kr.hhplus.be.server.order.dto.OrderItemRequest;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.repository.ProductRepository;
import kr.hhplus.be.server.support.TestDataHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 분산락 통합 테스트
 * - Redis TestContainers 사용
 * - 실제 동시성 제어 검증
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class DistributedLockIntegrationTest {

    @Autowired
    private CreateOrderUseCase createOrderUseCase;
    
    @Autowired
    private ChargeBalanceUseCase chargeBalanceUseCase;
    
    @Autowired
    private IssueCouponUseCase issueCouponUseCase;
    
    @Autowired
    private TestDataHelper testDataHelper;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserBalanceRepository userBalanceRepository;
    
    @Autowired
    private CouponRepository couponRepository;

    private Product testProduct;
    private UserBalance testUserBalance;
    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 생성 - Setter 사용 금지 원칙 준수
        testProduct = testDataHelper.createTestProduct();
        testUserBalance = testDataHelper.createTestUserBalance(1L);
        testCoupon = testDataHelper.createTestCoupon();
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 후 데이터 정리는 하지 않음 (실제 동시성 테스트를 위해)
        // TestContainers가 자동으로 격리를 제공
    }

    @Test
    @DisplayName("동시 주문 처리 시 분산락으로 재고 정확성 보장")
    void 동시_주문_처리시_분산락으로_재고_정확성_보장() throws InterruptedException {
        // Given
        int threadCount = 10;
        int orderQuantity = 5; // 각각 5개씩 주문
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // 테스트용 사용자들 미리 생성 (Setter 사용 금지 원칙 준수)
        testDataHelper.createMultipleUserBalances(threadCount, BigDecimal.valueOf(100000));

        // When
        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1;
            executorService.submit(() -> {
                try {
                    
                    // 주문 요청 생성
                    CreateOrderRequest orderRequest = new CreateOrderRequest(
                            userId,
                            List.of(new OrderItemRequest(testProduct.getId(), orderQuantity)),
                            null // 쿠폰 없음
                    );
                    
                    createOrderUseCase.execute(orderRequest);
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("주문 실패: " + e.getMessage());
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        // Then
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        
        // 성공한 주문만큼 재고가 차감되어야 함
        int expectedRemainingStock = 100 - (successCount.get() * orderQuantity);
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(expectedRemainingStock);
        
        // 전체 주문 수량이 재고를 초과하므로 일부는 실패해야 함
        assertThat(successCount.get()).isLessThanOrEqualTo(20); // 100 / 5 = 20
        assertThat(failCount.get()).isGreaterThan(0);
        
        System.out.println("성공: " + successCount.get() + ", 실패: " + failCount.get() + 
                         ", 남은 재고: " + updatedProduct.getStockQuantity());
    }

    @Test
    @DisplayName("동시 쿠폰 발급 시 분산락으로 중복 발급 방지")
    void 동시_쿠폰_발급시_분산락으로_중복_발급_방지() throws InterruptedException {
        // Given
        int threadCount = 15; // 발급 가능 수량(10)보다 많은 요청
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // When
        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1;
            executorService.submit(() -> {
                try {
                    issueCouponUseCase.execute(testCoupon.getId(), userId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("쿠폰 발급 실패: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        // Then
        Coupon updatedCoupon = couponRepository.findById(testCoupon.getId()).orElseThrow();
        
        // 발급 가능 수량만큼만 성공해야 함
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(5);
        assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(10);
        
        System.out.println("쿠폰 발급 성공: " + successCount.get() + ", 실패: " + failCount.get() + 
                         ", 발급된 수량: " + updatedCoupon.getIssuedQuantity());
    }

    @Test
    @DisplayName("동시 잔액 충전 시 분산락으로 정확성 보장")
    void 동시_잔액_충전시_분산락으로_정확성_보장() throws InterruptedException {
        // Given
        int threadCount = 10;
        BigDecimal chargeAmount = BigDecimal.valueOf(10000);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();

        // When
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    chargeBalanceUseCase.execute(testUserBalance.getUserId(), chargeAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.out.println("잔액 충전 실패: " + e.getMessage());
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        // Then
        UserBalance updatedBalance = userBalanceRepository.findByUserId(testUserBalance.getUserId()).orElseThrow();
        
        // 초기 잔액 + (성공한 충전 횟수 * 충전 금액)
        BigDecimal expectedBalance = BigDecimal.valueOf(1000000)
                .add(chargeAmount.multiply(BigDecimal.valueOf(successCount.get())));
        
        assertThat(updatedBalance.getBalance()).isEqualByComparingTo(expectedBalance);
        assertThat(successCount.get()).isEqualTo(threadCount); // 모든 충전이 성공해야 함
        
        System.out.println("잔액 충전 성공: " + successCount.get() + 
                         ", 최종 잔액: " + updatedBalance.getBalance());
    }
}