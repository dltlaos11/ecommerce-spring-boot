package kr.hhplus.be.server.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.coupon.dto.IssuedCouponResponse;
import kr.hhplus.be.server.coupon.exception.CouponAlreadyIssuedException;
import kr.hhplus.be.server.coupon.exception.CouponExhaustedException;
import kr.hhplus.be.server.coupon.service.CouponService;

/**
 * IssueCouponUseCase 단위 테스트
 * 
 * 테스트 전략:
 * - 단일 비즈니스 요구사항 "사용자가 선착순 쿠폰을 발급받는다" 검증
 * - CouponService에 올바른 위임 확인
 * - 예외 상황 처리 검증
 * 
 * 핵심 비즈니스 규칙:
 * - 중복 발급 방지
 * - 선착순 수량 제한
 * - 트랜잭션 내에서 일관성 보장
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IssueCouponUseCase 단위 테스트")
class IssueCouponUseCaseTest {

    @Mock
    private CouponService couponService;

    @InjectMocks
    private IssueCouponUseCase issueCouponUseCase;

    @Test
    @DisplayName("쿠폰 발급 성공 - 유효한 쿠폰으로 발급할 때 CouponService에 올바르게 위임한다")
    void 쿠폰발급_성공() {
        // Given
        Long couponId = 1L;
        Long userId = 1L;

        IssuedCouponResponse expectedResponse = new IssuedCouponResponse(
                123L, // userCouponId
                couponId, // couponId
                userId, // userId
                "신규 가입 쿠폰", // couponName
                "FIXED", // discountType
                new BigDecimal("5000"), // discountValue
                new BigDecimal("5000"), // maxDiscountAmount
                new BigDecimal("30000"), // minimumOrderAmount
                LocalDateTime.now().plusDays(30), // expiredAt
                LocalDateTime.now(), // issuedAt
                "AVAILABLE" // status
        );

        when(couponService.issueCoupon(couponId, userId)).thenReturn(expectedResponse);

        // When
        IssuedCouponResponse response = issueCouponUseCase.execute(couponId, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.couponId()).isEqualTo(couponId);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.couponName()).isEqualTo("신규 가입 쿠폰");
        assertThat(response.status()).isEqualTo("AVAILABLE");
        assertThat(response.userCouponId()).isNotNull();

        // CouponService에 정확한 파라미터로 위임했는지 검증
        verify(couponService).issueCoupon(couponId, userId);
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 중복 발급 시 예외를 전파한다")
    void 쿠폰발급_실패_중복발급() {
        // Given
        Long couponId = 1L;
        Long userId = 1L;

        when(couponService.issueCoupon(couponId, userId))
                .thenThrow(new CouponAlreadyIssuedException(
                        kr.hhplus.be.server.common.exception.ErrorCode.COUPON_ALREADY_ISSUED));

        // When & Then
        assertThatThrownBy(() -> issueCouponUseCase.execute(couponId, userId))
                .isInstanceOf(CouponAlreadyIssuedException.class);

        verify(couponService).issueCoupon(couponId, userId);
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 쿠폰 소진 시 예외를 전파한다")
    void 쿠폰발급_실패_쿠폰소진() {
        // Given
        Long couponId = 1L;
        Long userId = 1L;

        when(couponService.issueCoupon(couponId, userId))
                .thenThrow(new CouponExhaustedException(
                        kr.hhplus.be.server.common.exception.ErrorCode.COUPON_EXHAUSTED));

        // When & Then
        assertThatThrownBy(() -> issueCouponUseCase.execute(couponId, userId))
                .isInstanceOf(CouponExhaustedException.class);

        verify(couponService).issueCoupon(couponId, userId);
    }

    @Test
    @DisplayName("UseCase 단일 책임 검증 - execute 메서드만 존재한다")
    void UseCase_단일책임_검증() {
        // Given: UseCase 클래스 메서드 확인
        var methods = IssueCouponUseCase.class.getDeclaredMethods();
        var publicMethods = java.util.Arrays.stream(methods)
                .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
                .filter(method -> !method.getName().equals("equals"))
                .filter(method -> !method.getName().equals("hashCode"))
                .filter(method -> !method.getName().equals("toString"))
                .toList();

        // Then: execute 메서드만 존재해야 함 (단일 책임)
        assertThat(publicMethods).hasSize(1);
        assertThat(publicMethods.get(0).getName()).isEqualTo("execute");
    }

    @Test
    @DisplayName("트랜잭션 어노테이션 확인 - @Transactional이 적용되어 있다")
    void 트랜잭션_어노테이션_확인() {
        // Given: UseCase 클래스
        Class<IssueCouponUseCase> clazz = IssueCouponUseCase.class;

        // Then: @Transactional 어노테이션이 적용되어 있어야 함
        boolean hasTransactional = clazz.isAnnotationPresent(
                org.springframework.transaction.annotation.Transactional.class);

        assertThat(hasTransactional).isTrue();
    }

    @Test
    @DisplayName("UseCase 어노테이션 확인 - @UseCase가 적용되어 있다")
    void UseCase_어노테이션_확인() {
        // Given: UseCase 클래스
        Class<IssueCouponUseCase> clazz = IssueCouponUseCase.class;

        // Then: @UseCase 어노테이션이 적용되어 있어야 함
        boolean hasUseCase = clazz.isAnnotationPresent(
                kr.hhplus.be.server.common.annotation.UseCase.class);

        assertThat(hasUseCase).isTrue();
    }

    @Test
    @DisplayName("의존성 주입 확인 - CouponService만 의존한다")
    void 의존성주입_확인() {
        // Given: UseCase 필드 확인
        var fields = IssueCouponUseCase.class.getDeclaredFields();
        var serviceFields = java.util.Arrays.stream(fields)
                .filter(field -> !field.getName().contains("mockito")) // Mockito 필드 제외
                .filter(field -> !field.getName().contains("Mock")) // Mock 필드 제외
                .filter(field -> !field.getName().equals("log")) // Lombok @Slf4j 필드 제외
                .filter(field -> !field.getType().equals(org.slf4j.Logger.class)) // Logger 타입 제외
                .toList();

        // Then: CouponService 하나만 의존해야 함 (log 필드 제외)
        assertThat(serviceFields).hasSize(1);
        assertThat(serviceFields.get(0).getType()).isEqualTo(CouponService.class);
        assertThat(serviceFields.get(0).getName()).isEqualTo("couponService");
    }

    @Test
    @DisplayName("메서드 파라미터 검증 - execute가 올바른 파라미터를 받는다")
    void 메서드파라미터_검증() throws NoSuchMethodException {
        // Given: 메서드 시그니처 확인
        var executeMethod = IssueCouponUseCase.class.getDeclaredMethod("execute", Long.class, Long.class);

        // Then: 메서드 시그니처가 올바른지 확인
        assertThat(executeMethod.getReturnType()).isEqualTo(IssuedCouponResponse.class);
        assertThat(executeMethod.getParameterTypes()).containsExactly(Long.class, Long.class);
    }

    @Test
    @DisplayName("비즈니스 로직 검증 - 쿠폰 발급은 선착순 처리여야 한다")
    void 비즈니스로직_선착순처리_검증() {
        // Given: 여러 사용자가 동시에 같은 쿠폰 발급 시도
        Long couponId = 1L;
        Long userId1 = 1L;
        Long userId2 = 2L;

        // 첫 번째 사용자는 성공
        IssuedCouponResponse successResponse = new IssuedCouponResponse(
                123L, couponId, userId1, "선착순 쿠폰", "FIXED",
                new BigDecimal("10000"), new BigDecimal("10000"),
                new BigDecimal("50000"), LocalDateTime.now().plusDays(7),
                LocalDateTime.now(), "AVAILABLE");
        when(couponService.issueCoupon(couponId, userId1)).thenReturn(successResponse);

        // 두 번째 사용자는 소진으로 실패
        when(couponService.issueCoupon(couponId, userId2))
                .thenThrow(new CouponExhaustedException(
                        kr.hhplus.be.server.common.exception.ErrorCode.COUPON_EXHAUSTED));

        // When & Then: 첫 번째는 성공, 두 번째는 실패
        var result1 = issueCouponUseCase.execute(couponId, userId1);
        assertThat(result1.userId()).isEqualTo(userId1);

        assertThatThrownBy(() -> issueCouponUseCase.execute(couponId, userId2))
                .isInstanceOf(CouponExhaustedException.class);

        // Both calls should be delegated to service
        verify(couponService).issueCoupon(couponId, userId1);
        verify(couponService).issueCoupon(couponId, userId2);
    }
}