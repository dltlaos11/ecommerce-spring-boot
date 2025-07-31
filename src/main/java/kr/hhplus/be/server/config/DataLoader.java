package kr.hhplus.be.server.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.repository.CouponRepository;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MySQL 연동 후 초기 데이터 로딩
 * 
 * 변경사항:
 * - JPA Repository를 통한 실제 MySQL 데이터 저장
 * - @PostConstruct 대신 ApplicationRunner 사용 (트랜잭션 안전성)
 * - Product, Coupon 초기 데이터 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        log.info("🏪 MySQL 초기 데이터 로딩 시작...");

        // 기존 데이터가 있는지 확인
        if (productRepository.findAll().isEmpty()) {
            initializeProducts();
            log.info("✅ 상품 초기 데이터 생성 완료!");
        } else {
            log.info("📦 기존 상품 데이터 존재, 초기화 생략");
        }

        if (couponRepository.findAll().isEmpty()) {
            initializeCoupons();
            log.info("✅ 쿠폰 초기 데이터 생성 완료!");
        } else {
            log.info("🎫 기존 쿠폰 데이터 존재, 초기화 생략");
        }

        log.info("🎉 MySQL 초기 데이터 로딩 완료!");
    }

    /**
     * 상품 초기 데이터 생성
     */
    private void initializeProducts() {
        log.info("📦 상품 초기 데이터 생성 중...");

        productRepository.save(new Product("고성능 노트북", new BigDecimal("1500000"), 10));
        productRepository.save(new Product("무선 마우스", new BigDecimal("50000"), 50));
        productRepository.save(new Product("기계식 키보드", new BigDecimal("150000"), 25));
        productRepository.save(new Product("27인치 모니터", new BigDecimal("300000"), 15));
        productRepository.save(new Product("HD 웹캠", new BigDecimal("80000"), 30));
        productRepository.save(new Product("게이밍 의자", new BigDecimal("400000"), 8));
        productRepository.save(new Product("무선 헤드셋", new BigDecimal("120000"), 20));
        productRepository.save(new Product("USB 허브", new BigDecimal("30000"), 40));
        productRepository.save(new Product("스마트폰 거치대", new BigDecimal("25000"), 100));
        productRepository.save(new Product("블루투스 스피커", new BigDecimal("200000"), 12));

        log.info("💾 10개 상품 초기 데이터 저장 완료");
    }

    /**
     * 쿠폰 초기 데이터 생성
     */
    private void initializeCoupons() {
        log.info("🎫 쿠폰 초기 데이터 생성 중...");

        // 1. 신규 가입 쿠폰 (정액 할인)
        couponRepository.save(new Coupon(
                "신규 가입 쿠폰",
                Coupon.DiscountType.FIXED,
                new BigDecimal("5000"),
                100,
                new BigDecimal("5000"),
                new BigDecimal("30000"),
                LocalDateTime.now().plusDays(30)));

        // 2. VIP 10% 할인 쿠폰 (정률 할인)
        couponRepository.save(new Coupon(
                "VIP 10% 할인 쿠폰",
                Coupon.DiscountType.PERCENTAGE,
                new BigDecimal("10"),
                50,
                new BigDecimal("50000"),
                new BigDecimal("100000"),
                LocalDateTime.now().plusDays(7)));

        // 3. 선착순 20% 할인 (제한된 수량)
        couponRepository.save(new Coupon(
                "선착순 20% 할인",
                Coupon.DiscountType.PERCENTAGE,
                new BigDecimal("20"),
                10, // 적은 수량으로 선착순 테스트
                new BigDecimal("100000"),
                new BigDecimal("50000"),
                LocalDateTime.now().plusDays(3)));

        // 4. 첫 주문 15% 할인
        couponRepository.save(new Coupon(
                "첫 주문 15% 할인",
                Coupon.DiscountType.PERCENTAGE,
                new BigDecimal("15"),
                200,
                new BigDecimal("30000"),
                new BigDecimal("20000"),
                LocalDateTime.now().plusDays(14)));

        // 5. 고액 주문 10만원 할인
        couponRepository.save(new Coupon(
                "고액 주문 10만원 할인",
                Coupon.DiscountType.FIXED,
                new BigDecimal("100000"),
                30,
                new BigDecimal("100000"),
                new BigDecimal("1000000"),
                LocalDateTime.now().plusDays(21)));

        // 6. 만료된 쿠폰 (테스트용)
        couponRepository.save(new Coupon(
                "만료된 테스트 쿠폰",
                Coupon.DiscountType.FIXED,
                new BigDecimal("10000"),
                50,
                new BigDecimal("10000"),
                new BigDecimal("20000"),
                LocalDateTime.now().minusDays(1) // 이미 만료됨
        ));

        log.info("💾 6개 쿠폰 초기 데이터 저장 완료");
    }
}