package kr.hhplus.be.server.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.coupon.dto.AvailableCouponResponse;
import kr.hhplus.be.server.coupon.dto.CouponValidationResponse;
import kr.hhplus.be.server.coupon.dto.IssuedCouponResponse;
import kr.hhplus.be.server.coupon.dto.UserCouponResponse;
import kr.hhplus.be.server.coupon.exception.CouponAlreadyIssuedException;
import kr.hhplus.be.server.coupon.exception.CouponExhaustedException;
import kr.hhplus.be.server.coupon.exception.CouponExpiredException;
import kr.hhplus.be.server.coupon.exception.CouponNotFoundException;
import kr.hhplus.be.server.coupon.repository.CouponRepository;
import kr.hhplus.be.server.coupon.repository.UserCouponRepository;

/**
 * CouponService 단위 테스트
 * 
 * 테스트 전략:
 * - Mock을 활용한 외부 의존성 격리
 * - 비즈니스 규칙 검증 (중복 발급 방지, 만료일, 수량 제한)
 * - 할인 금액 계산 로직 검증
 * - 쿠폰 상태 관리 테스트
 * 
 * 테스트 범위:
 * - 쿠폰 발급/조회/검증
 * - 도메인 로직 검증
 * - DTO 변환 로직
 * - 예외 처리 시나리오
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CouponService 단위 테스트 (STEP05)")
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @InjectMocks
    private CouponService couponService;

    @Test
    @DisplayName("발급 가능한 쿠폰 목록 조회 성공")
    void 발급가능한쿠폰목록조회_성공() {
        // Given
        List<Coupon> mockCoupons = List.of(
                createTestCoupon(1L, "신규 가입 쿠폰", Coupon.DiscountType.FIXED, "5000", 100, 10),
                createTestCoupon(2L, "VIP 할인 쿠폰", Coupon.DiscountType.PERCENTAGE, "10", 50, 5));

        when(couponRepository.findAvailableCoupons()).thenReturn(mockCoupons);

        // When
        List<AvailableCouponResponse> responses = couponService.getAvailableCoupons();

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(AvailableCouponResponse::name)
                .containsExactly("신규 가입 쿠폰", "VIP 할인 쿠폰");

        verify(couponRepository).findAvailableCoupons();
    }

    @Test
    @DisplayName("쿠폰 발급 성공 - 유효한 쿠폰으로 발급할 때 성공한다")
    void 쿠폰발급_성공() {
        // Given
        Long couponId = 1L;
        Long userId = 1L;
        Coupon coupon = createTestCoupon(couponId, "신규 가입 쿠폰", Coupon.DiscountType.FIXED, "5000", 100, 10);

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(Optional.empty());
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);
        when(userCouponRepository.save(any(UserCoupon.class))).thenReturn(createTestUserCoupon(1L, userId, couponId));

        // When
        IssuedCouponResponse response = couponService.issueCoupon(couponId, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.couponId()).isEqualTo(couponId);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.couponName()).isEqualTo("신규 가입 쿠폰");
        assertThat(response.status()).isEqualTo("AVAILABLE");

        // 발급 수량이 증가했는지 확인
        assertThat(coupon.getIssuedQuantity()).isEqualTo(11);

        verify(couponRepository).findById(couponId);
        verify(userCouponRepository).findByUserIdAndCouponId(userId, couponId);
        verify(couponRepository).save(coupon);
        verify(userCouponRepository).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 존재하지 않는 쿠폰으로 발급할 때 예외가 발생한다")
    void 쿠폰발급_실패_쿠폰없음() {
        // Given
        Long couponId = 999L;
        Long userId = 1L;

        when(couponRepository.findById(couponId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> couponService.issueCoupon(couponId, userId))
                .isInstanceOf(CouponNotFoundException.class);

        verify(couponRepository).findById(couponId);
        verify(userCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 이미 발급받은 쿠폰으로 발급할 때 예외가 발생한다")
    void 쿠폰발급_실패_중복발급() {
        // Given
        Long couponId = 1L;
        Long userId = 1L;
        Coupon coupon = createTestCoupon(couponId, "신규 가입 쿠폰", Coupon.DiscountType.FIXED, "5000", 100, 10);
        UserCoupon existingUserCoupon = createTestUserCoupon(1L, userId, couponId);

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .thenReturn(Optional.of(existingUserCoupon));

        // When & Then
        assertThatThrownBy(() -> couponService.issueCoupon(couponId, userId))
                .isInstanceOf(CouponAlreadyIssuedException.class);

        verify(couponRepository).findById(couponId);
        verify(userCouponRepository).findByUserIdAndCouponId(userId, couponId);
        verify(couponRepository, never()).save(any());
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 소진된 쿠폰으로 발급할 때 예외가 발생한다")
    void 쿠폰발급_실패_쿠폰소진() {
        // Given
        Long couponId = 1L;
        Long userId = 1L;
        Coupon exhaustedCoupon = createTestCoupon(couponId, "소진된 쿠폰", Coupon.DiscountType.FIXED, "5000", 10, 10); // 이미
                                                                                                                  // 모두
                                                                                                                  // 발급됨

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(exhaustedCoupon));
        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> couponService.issueCoupon(couponId, userId))
                .isInstanceOf(CouponExhaustedException.class);

        verify(couponRepository, never()).save(any());
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 만료된 쿠폰으로 발급할 때 예외가 발생한다")
    void 쿠폰발급_실패_쿠폰만료() {
        // Given
        Long couponId = 1L;
        Long userId = 1L;
        Coupon expiredCoupon = createExpiredCoupon(couponId, "만료된 쿠폰");

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(expiredCoupon));
        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> couponService.issueCoupon(couponId, userId))
                .isInstanceOf(CouponExpiredException.class);

        verify(couponRepository, never()).save(any());
    }

    @Test
    @DisplayName("사용자 쿠폰 목록 조회 성공")
    void 사용자쿠폰목록조회_성공() {
        // Given
        Long userId = 1L;
        List<UserCoupon> userCoupons = List.of(
                createTestUserCoupon(1L, userId, 1L),
                createTestUserCoupon(2L, userId, 2L));

        Coupon coupon1 = createTestCoupon(1L, "쿠폰1", Coupon.DiscountType.FIXED, "5000", 100, 10);
        Coupon coupon2 = createTestCoupon(2L, "쿠폰2", Coupon.DiscountType.PERCENTAGE, "10", 50, 5);

        when(userCouponRepository.findByUserId(userId)).thenReturn(userCoupons);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon1));
        when(couponRepository.findById(2L)).thenReturn(Optional.of(coupon2));

        // When
        List<UserCouponResponse> responses = couponService.getUserCoupons(userId);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(UserCouponResponse::couponName)
                .containsExactly("쿠폰1", "쿠폰2");

        verify(userCouponRepository).findByUserId(userId);
        verify(couponRepository, times(2)).findById(any());
    }

    @Test
    @DisplayName("쿠폰 검증 성공 - 사용 가능한 쿠폰으로 검증할 때 할인 금액을 계산한다")
    void 쿠폰검증_성공_정률할인() {
        // Given
        Long userId = 1L;
        Long couponId = 1L;
        BigDecimal orderAmount = new BigDecimal("150000");

        // 10% 할인, 최대 50,000원, 최소 주문 100,000원
        Coupon coupon = createTestCoupon(couponId, "10% 할인 쿠폰", Coupon.DiscountType.PERCENTAGE, "10", 50, 10);
        coupon.setMaxDiscountAmountForTest(new BigDecimal("50000"));
        coupon.setMinimumOrderAmountForTest(new BigDecimal("100000"));

        UserCoupon userCoupon = createTestUserCoupon(1L, userId, couponId);

        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .thenReturn(Optional.of(userCoupon));
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // When
        CouponValidationResponse response = couponService.validateAndCalculateDiscount(userId, couponId, orderAmount);

        // Then
        assertThat(response.usable()).isTrue();
        assertThat(response.discountAmount()).isEqualByComparingTo(new BigDecimal("15000")); // 150,000 * 10% = 15,000
        assertThat(response.finalAmount()).isEqualByComparingTo(new BigDecimal("135000")); // 150,000 - 15,000
        assertThat(response.reason()).isNull();

        verify(userCouponRepository).findByUserIdAndCouponId(userId, couponId);
        verify(couponRepository).findById(couponId);
    }

    @Test
    @DisplayName("쿠폰 검증 실패 - 최소 주문 금액 미달 시 사용 불가")
    void 쿠폰검증_실패_최소주문금액미달() {
        // Given
        Long userId = 1L;
        Long couponId = 1L;
        BigDecimal orderAmount = new BigDecimal("50000"); // 최소 주문 금액(100,000원) 미달

        Coupon coupon = createTestCoupon(couponId, "10% 할인 쿠폰", Coupon.DiscountType.PERCENTAGE, "10", 50, 10);
        coupon.setMinimumOrderAmountForTest(new BigDecimal("100000"));

        UserCoupon userCoupon = createTestUserCoupon(1L, userId, couponId);

        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .thenReturn(Optional.of(userCoupon));
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // When
        CouponValidationResponse response = couponService.validateAndCalculateDiscount(userId, couponId, orderAmount);

        // Then
        assertThat(response.usable()).isFalse();
        assertThat(response.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.finalAmount()).isEqualByComparingTo(orderAmount);
        assertThat(response.reason()).contains("최소 주문 금액");
    }

    @Test
    @DisplayName("쿠폰 검증 실패 - 보유하지 않은 쿠폰으로 검증할 때 사용 불가")
    void 쿠폰검증_실패_보유하지않은쿠폰() {
        // Given
        Long userId = 1L;
        Long couponId = 1L;
        BigDecimal orderAmount = new BigDecimal("150000");

        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .thenReturn(Optional.empty());

        // When
        CouponValidationResponse response = couponService.validateAndCalculateDiscount(userId, couponId, orderAmount);

        // Then
        assertThat(response.usable()).isFalse();
        assertThat(response.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.finalAmount()).isEqualByComparingTo(orderAmount);
        assertThat(response.reason()).contains("보유하지 않은 쿠폰");
    }

    @Test
    @DisplayName("쿠폰 사용 성공 - 유효한 쿠폰으로 사용할 때 할인 금액을 반환한다")
    void 쿠폰사용_성공() {
        // Given
        Long userId = 1L;
        Long couponId = 1L;
        BigDecimal orderAmount = new BigDecimal("100000");

        // 5000원 정액 할인
        Coupon coupon = createTestCoupon(couponId, "5000원 할인", Coupon.DiscountType.FIXED, "5000", 100, 10);
        UserCoupon userCoupon = createTestUserCoupon(1L, userId, couponId);

        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .thenReturn(Optional.of(userCoupon));
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(userCouponRepository.save(any(UserCoupon.class))).thenReturn(userCoupon);

        // When
        BigDecimal discountAmount = couponService.useCoupon(userId, couponId, orderAmount);

        // Then
        assertThat(discountAmount).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(userCoupon.getStatus()).isEqualTo(UserCoupon.CouponStatus.USED);
        assertThat(userCoupon.getUsedAt()).isNotNull();

        verify(userCouponRepository).findByUserIdAndCouponId(userId, couponId);
        verify(couponRepository).findById(couponId);
        verify(userCouponRepository).save(userCoupon);
    }

    @Test
    @DisplayName("특정 쿠폰 조회 성공")
    void 특정쿠폰조회_성공() {
        // Given
        Long couponId = 1L;
        Coupon coupon = createTestCoupon(couponId, "테스트 쿠폰", Coupon.DiscountType.FIXED, "5000", 100, 10);

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // When
        AvailableCouponResponse response = couponService.getCoupon(couponId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(couponId);
        assertThat(response.name()).isEqualTo("테스트 쿠폰");
        assertThat(response.discountType()).isEqualTo("FIXED");

        verify(couponRepository).findById(couponId);
    }

    @Test
    @DisplayName("특정 쿠폰 조회 실패 - 존재하지 않는 쿠폰 조회 시 예외가 발생한다")
    void 특정쿠폰조회_실패_쿠폰없음() {
        // Given
        Long couponId = 999L;

        when(couponRepository.findById(couponId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> couponService.getCoupon(couponId))
                .isInstanceOf(CouponNotFoundException.class);

        verify(couponRepository).findById(couponId);
    }

    /**
     * 테스트용 Coupon 객체 생성 헬퍼 메서드
     */
    private Coupon createTestCoupon(Long id, String name, Coupon.DiscountType discountType,
            String discountValue, Integer totalQuantity, Integer issuedQuantity) {
        Coupon coupon = new Coupon(
                name,
                discountType,
                new BigDecimal(discountValue),
                totalQuantity,
                new BigDecimal("100000"), // maxDiscountAmount
                new BigDecimal("0"), // minimumOrderAmount
                LocalDateTime.now().plusDays(30) // expiredAt
        );
        coupon.setIdForTest(id);
        coupon.setIssuedQuantityForTest(issuedQuantity);
        coupon.setCreatedAtForTest(LocalDateTime.now());
        coupon.setUpdatedAtForTest(LocalDateTime.now());
        return coupon;
    }

    /**
     * 테스트용 만료된 Coupon 객체 생성 헬퍼 메서드
     */
    private Coupon createExpiredCoupon(Long id, String name) {
        Coupon coupon = new Coupon(
                name,
                Coupon.DiscountType.FIXED,
                new BigDecimal("5000"),
                100,
                new BigDecimal("5000"),
                new BigDecimal("0"),
                LocalDateTime.now().minusDays(1) // 이미 만료됨
        );
        coupon.setIdForTest(id);
        coupon.setIssuedQuantityForTest(10);
        return coupon;
    }

    /**
     * 테스트용 UserCoupon 객체 생성 헬퍼 메서드
     */
    private UserCoupon createTestUserCoupon(Long id, Long userId, Long couponId) {
        UserCoupon userCoupon = new UserCoupon(userId, couponId);
        userCoupon.setIdForTest(id);
        userCoupon.setCreatedAtForTest(LocalDateTime.now());
        userCoupon.setUpdatedAtForTest(LocalDateTime.now());
        return userCoupon;
    }
}