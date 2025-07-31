package kr.hhplus.be.server.coupon.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.common.test.IntegrationTestBase;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.dto.CouponValidationRequest;
import kr.hhplus.be.server.coupon.dto.IssueCouponRequest;
import kr.hhplus.be.server.coupon.repository.CouponRepository;
import kr.hhplus.be.server.coupon.repository.UserCouponRepository;

/**
 * CouponController 통합 테스트
 * 
 * 테스트 범위:
 * - 쿠폰 조회/발급 API
 * - 선착순 쿠폰 발급 테스트
 * - 쿠폰 검증 API
 * - 사용자 쿠폰 목록 조회
 */
@DisplayName("쿠폰 관리 통합 테스트")
@Transactional
class CouponControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Test
    @DisplayName("발급 가능한 쿠폰 목록 조회 통합 테스트")
    void 발급가능한쿠폰목록조회_통합테스트() {
        // Given: DataLoader에 의해 초기 쿠폰 데이터가 있음

        // When: 발급 가능한 쿠폰 목록 조회 API 호출
        ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                "/api/v1/coupons/available",
                CommonResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        // DB 검증 - DataLoader에서 생성한 쿠폰들이 있어야 함
        var availableCoupons = couponRepository.findAvailableCoupons();
        assertThat(availableCoupons).isNotEmpty();
    }

    @Test
    @DisplayName("특정 쿠폰 조회 통합 테스트")
    void 특정쿠폰조회_통합테스트() {
        // Given: 테스트용 쿠폰 생성
        Coupon testCoupon = createTestCoupon("통합테스트쿠폰", new BigDecimal("5000"));
        Coupon savedCoupon = couponRepository.save(testCoupon);

        // When: 특정 쿠폰 조회 API 호출
        ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                "/api/v1/coupons/{couponId}",
                CommonResponse.class,
                savedCoupon.getId());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        // DB 검증
        var foundCoupon = couponRepository.findById(savedCoupon.getId());
        assertThat(foundCoupon).isPresent();
        assertThat(foundCoupon.get().getName()).isEqualTo("통합테스트쿠폰");
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 조회 시 404 에러")
    void 존재하지않는쿠폰조회_404에러() {
        // Given: 존재하지 않는 쿠폰 ID
        Long nonExistentId = 99999L;

        // When
        ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                "/api/v1/coupons/{couponId}",
                CommonResponse.class,
                nonExistentId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    @DisplayName("쿠폰 발급 통합 테스트 - 정상 발급 시나리오")
    void 쿠폰발급_통합테스트() {
        // Given: 테스트용 쿠폰 생성
        Coupon testCoupon = createTestCoupon("발급테스트쿠폰", new BigDecimal("10000"));
        Coupon savedCoupon = couponRepository.save(testCoupon);

        Long userId = generateUniqueUserId();
        IssueCouponRequest request = new IssueCouponRequest(userId);

        // When: 쿠폰 발급 API 호출
        ResponseEntity<CommonResponse> response = restTemplate.postForEntity(
                "/api/v1/coupons/{couponId}/issue",
                request,
                CommonResponse.class,
                savedCoupon.getId());

        // Then: HTTP 응답 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().isSuccess()).isTrue();

        // DB 상태 검증
        // 1. 쿠폰 발급 수량 증가 확인
        var updatedCoupon = couponRepository.findById(savedCoupon.getId());
        assertThat(updatedCoupon).isPresent();
        assertThat(updatedCoupon.get().getIssuedQuantity()).isEqualTo(1);

        // 2. 사용자 쿠폰 생성 확인
        var userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, savedCoupon.getId());
        assertThat(userCoupon).isPresent();
        assertThat(userCoupon.get().getStatus()).isEqualTo(
                kr.hhplus.be.server.coupon.domain.UserCoupon.CouponStatus.AVAILABLE);
    }

    @Test
    @DisplayName("쿠폰 중복 발급 시 409 에러 발생")
    void 쿠폰중복발급_409에러() {
        // Given: 쿠폰 생성 및 첫 번째 발급
        Coupon testCoupon = createTestCoupon("중복발급테스트쿠폰", new BigDecimal("5000"));
        Coupon savedCoupon = couponRepository.save(testCoupon);

        Long userId = generateUniqueUserId();
        IssueCouponRequest request = new IssueCouponRequest(userId);

        // 첫 번째 발급 (성공해야 함)
        restTemplate.postForEntity(
                "/api/v1/coupons/{couponId}/issue",
                request,
                CommonResponse.class,
                savedCoupon.getId());

        // When: 같은 쿠폰 재발급 시도
        ResponseEntity<CommonResponse> response = restTemplate.postForEntity(
                "/api/v1/coupons/{couponId}/issue",
                request,
                CommonResponse.class,
                savedCoupon.getId());

        // Then: 중복 발급 에러
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().isSuccess()).isFalse();

        // DB 검증 - 발급 수량이 1개 그대로여야 함
        var coupon = couponRepository.findById(savedCoupon.getId());
        assertThat(coupon.get().getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("사용자 보유 쿠폰 목록 조회 통합 테스트")
    void 사용자보유쿠폰목록조회_통합테스트() {
        // Given: 사용자가 여러 쿠폰을 발급받음
        Long userId = generateUniqueUserId();

        // 첫 번째 쿠폰 발급
        Coupon coupon1 = createTestCoupon("사용자테스트쿠폰1", new BigDecimal("5000"));
        Coupon savedCoupon1 = couponRepository.save(coupon1);
        restTemplate.postForEntity(
                "/api/v1/coupons/{couponId}/issue",
                new IssueCouponRequest(userId),
                CommonResponse.class,
                savedCoupon1.getId());

        // 두 번째 쿠폰 발급
        Coupon coupon2 = createTestCoupon("사용자테스트쿠폰2", new BigDecimal("10000"));
        Coupon savedCoupon2 = couponRepository.save(coupon2);
        restTemplate.postForEntity(
                "/api/v1/coupons/{couponId}/issue",
                new IssueCouponRequest(userId),
                CommonResponse.class,
                savedCoupon2.getId());

        // When: 사용자 쿠폰 목록 조회
        ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                "/api/v1/coupons/users/{userId}",
                CommonResponse.class,
                userId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        // DB 검증 - 2개 쿠폰이 있어야 함
        var userCoupons = userCouponRepository.findByUserId(userId);
        assertThat(userCoupons).hasSize(2);
    }

    @Test
    @DisplayName("쿠폰 검증 API 통합 테스트 - 사용 가능한 쿠폰")
    void 쿠폰검증API_통합테스트_사용가능() {
        // Given: 쿠폰 발급
        Coupon testCoupon = createTestCoupon("검증테스트쿠폰", new BigDecimal("10")); // 10% 할인
        testCoupon = new Coupon(
                "검증테스트쿠폰",
                Coupon.DiscountType.PERCENTAGE,
                new BigDecimal("10"),
                100,
                new BigDecimal("50000"), // 최대 할인 금액
                new BigDecimal("100000"), // 최소 주문 금액
                LocalDateTime.now().plusDays(30));
        Coupon savedCoupon = couponRepository.save(testCoupon);

        Long userId = generateUniqueUserId();
        restTemplate.postForEntity(
                "/api/v1/coupons/{couponId}/issue",
                new IssueCouponRequest(userId),
                CommonResponse.class,
                savedCoupon.getId());

        CouponValidationRequest validationRequest = new CouponValidationRequest(
                userId, savedCoupon.getId(), new BigDecimal("150000"));

        // When: 쿠폰 검증 API 호출
        ResponseEntity<CommonResponse> response = restTemplate.postForEntity(
                "/api/v1/coupons/validate",
                validationRequest,
                CommonResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
    }

    @Test
    @DisplayName("잘못된 쿠폰 발급 요청 시 400 에러")
    void 잘못된쿠폰발급요청_400에러() {
        // Given: 필수 필드 누락
        IssueCouponRequest invalidRequest = new IssueCouponRequest(null); // userId 누락

        // When
        ResponseEntity<CommonResponse> response = restTemplate.postForEntity(
                "/api/v1/coupons/{couponId}/issue",
                invalidRequest,
                CommonResponse.class,
                1L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    // ==================== 테스트 헬퍼 메서드들 ====================

    private Coupon createTestCoupon(String name, BigDecimal discountValue) {
        return new Coupon(
                name,
                Coupon.DiscountType.FIXED,
                discountValue,
                100, // 총 수량
                discountValue, // 최대 할인 금액
                BigDecimal.ZERO, // 최소 주문 금액
                LocalDateTime.now().plusDays(30) // 만료일
        );
    }
}