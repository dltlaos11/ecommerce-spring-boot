package kr.hhplus.be.server.support;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import kr.hhplus.be.server.balance.domain.UserBalance;
import kr.hhplus.be.server.balance.repository.UserBalanceRepository;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.repository.CouponRepository;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.repository.ProductRepository;

/**
 * 테스트 데이터 설정 헬퍼 - Setter 사용 금지 원칙 준수
 * 
 * - 생성자와 Repository를 통한 데이터 설정
 * - 비즈니스 로직을 통한 데이터 변경
 */
@Component
public class TestDataHelper {

    private final ProductRepository productRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final CouponRepository couponRepository;

    public TestDataHelper(ProductRepository productRepository,
            UserBalanceRepository userBalanceRepository,
            CouponRepository couponRepository) {
        this.productRepository = productRepository;
        this.userBalanceRepository = userBalanceRepository;
        this.couponRepository = couponRepository;
    }

    /**
     * 테스트용 상품 생성 (재고 있음)
     */
    public Product createTestProduct(String name, BigDecimal price, Integer stockQuantity) {
        return productRepository.save(new Product(name, price, stockQuantity));
    }

    /**
     * 기본 테스트 상품 생성
     */
    public Product createTestProduct() {
        return createTestProduct("테스트 상품", BigDecimal.valueOf(10000), 100);
    }

    /**
     * 테스트용 사용자 잔액 생성 후 충전
     */
    public UserBalance createUserBalanceWithAmount(Long userId, BigDecimal amount) {
        // 1. 기존 잔액이 있는지 확인
        var existingBalance = userBalanceRepository.findByUserId(userId);
        if (existingBalance.isPresent()) {
            UserBalance userBalance = existingBalance.get();
            // 기존 잔액에 추가 충전
            userBalance.charge(amount);
            return userBalanceRepository.save(userBalance);
        }

        // 2. 새로운 잔액 생성
        UserBalance userBalance = userBalanceRepository.save(new UserBalance(userId));

        // 3. 비즈니스 로직을 통한 잔액 충전
        userBalance.charge(amount);

        // 4. 변경사항 저장
        return userBalanceRepository.save(userBalance);
    }

    /**
     * 기본 테스트 사용자 잔액 생성 (100만원)
     */
    public UserBalance createTestUserBalance(Long userId) {
        return createUserBalanceWithAmount(userId, BigDecimal.valueOf(1000000));
    }

    /**
     * 테스트용 쿠폰 생성
     */
    public Coupon createTestCoupon(String name, Coupon.DiscountType discountType,
            BigDecimal discountValue, Integer totalQuantity,
            BigDecimal maxDiscountAmount, BigDecimal minimumOrderAmount,
            LocalDateTime expiredAt) {
        return couponRepository.save(new Coupon(name, discountType, discountValue,
                totalQuantity, maxDiscountAmount,
                minimumOrderAmount, expiredAt));
    }

    /**
     * 기본 테스트 쿠폰 생성 (정액 할인 1000원, 발급 가능 수량 10개)
     */
    public Coupon createTestCoupon() {
        return createTestCoupon("테스트 쿠폰", Coupon.DiscountType.FIXED,
                BigDecimal.valueOf(1000), 10,
                null, BigDecimal.ZERO,
                LocalDateTime.now().plusDays(30));
    }

    /**
     * 다수 사용자 잔액 생성
     */
    public void createMultipleUserBalances(int userCount, BigDecimal amountPerUser) {
        for (int i = 1; i <= userCount; i++) {
            createUserBalanceWithAmount((long) i, amountPerUser);
        }
    }

    /**
     * 재고 부족 상품 생성
     */
    public Product createLowStockProduct(Integer stockQuantity) {
        return createTestProduct("재고 부족 상품", BigDecimal.valueOf(10000), stockQuantity);
    }

    /**
     * 선착순 쿠폰 생성 (제한된 수량)
     */
    public Coupon createLimitedCoupon(Integer totalQuantity) {
        return createTestCoupon("선착순 쿠폰", Coupon.DiscountType.FIXED,
                BigDecimal.valueOf(1000), totalQuantity,
                null, BigDecimal.ZERO,
                LocalDateTime.now().plusDays(30));
    }
}