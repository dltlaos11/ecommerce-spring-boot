package kr.hhplus.be.server.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.coupon.dto.CouponValidationResponse;
import kr.hhplus.be.server.coupon.service.CouponService;

/**
 * ValidateCouponUseCase 단위 테스트
 * 
 * 테스트 전략:
 * - 단일 비즈니스 요구사항 "시스템이 쿠폰 사용 가능 여부를 검증한다" 검증
 * - CouponService에 올바른 위임 확인
 * - 할인 금액 계산 결과 검증
 * 
 * 핵심 비즈니스 규칙:
 * - 최소 주문 금액 검증
 * - 쿠폰 사용 가능 상태 확인
 * - 정확한 할인 금액 계산
 * - ReadOnly 트랜잭션으로 조회만 수행
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ValidateCouponUseCase 단위 테스트")
class ValidateCouponUseCaseTest {

        @Mock
        private CouponService couponService;

        @InjectMocks
        private ValidateCouponUseCase validateCouponUseCase;

        @Test
        @DisplayName("쿠폰 검증 성공 - 사용 가능한 쿠폰으로 검증할 때 할인 금액을 계산한다")
        void 쿠폰검증_성공() {
                // Given
                Long userId = 1L;
                Long couponId = 1L;
                BigDecimal orderAmount = new BigDecimal("150000");

                CouponValidationResponse expectedResponse = new CouponValidationResponse(
                                couponId, // couponId
                                userId, // userId
                                true, // usable
                                new BigDecimal("15000"), // discountAmount (10% 할인)
                                new BigDecimal("135000"), // finalAmount
                                null // reason (사용 가능하므로 null)
                );

                when(couponService.validateAndCalculateDiscount(userId, couponId, orderAmount))
                                .thenReturn(expectedResponse);

                // When
                CouponValidationResponse response = validateCouponUseCase.execute(userId, couponId, orderAmount);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.couponId()).isEqualTo(couponId);
                assertThat(response.userId()).isEqualTo(userId);
                assertThat(response.usable()).isTrue();
                assertThat(response.discountAmount()).isEqualByComparingTo(new BigDecimal("15000"));
                assertThat(response.finalAmount()).isEqualByComparingTo(new BigDecimal("135000"));
                assertThat(response.reason()).isNull();

                // CouponService에 정확한 파라미터로 위임했는지 검증
                verify(couponService).validateAndCalculateDiscount(userId, couponId, orderAmount);
        }

        @Test
        @DisplayName("쿠폰 검증 실패 - 최소 주문 금액 미달 시 사용 불가 응답한다")
        void 쿠폰검증_실패_최소주문금액미달() {
                // Given
                Long userId = 1L;
                Long couponId = 1L;
                BigDecimal orderAmount = new BigDecimal("50000"); // 최소 주문 금액 미달

                CouponValidationResponse expectedResponse = new CouponValidationResponse(
                                couponId, // couponId
                                userId, // userId
                                false, // usable
                                BigDecimal.ZERO, // discountAmount
                                orderAmount, // finalAmount (할인 없음)
                                "최소 주문 금액 100,000원 이상이어야 합니다." // reason
                );

                when(couponService.validateAndCalculateDiscount(userId, couponId, orderAmount))
                                .thenReturn(expectedResponse);

                // When
                CouponValidationResponse response = validateCouponUseCase.execute(userId, couponId, orderAmount);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.usable()).isFalse();
                assertThat(response.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(response.finalAmount()).isEqualByComparingTo(orderAmount);
                assertThat(response.reason()).contains("최소 주문 금액");

                verify(couponService).validateAndCalculateDiscount(userId, couponId, orderAmount);
        }

        @Test
        @DisplayName("쿠폰 검증 실패 - 보유하지 않은 쿠폰으로 검증할 때 사용 불가 응답한다")
        void 쿠폰검증_실패_보유하지않은쿠폰() {
                // Given
                Long userId = 1L;
                Long couponId = 999L; // 보유하지 않은 쿠폰
                BigDecimal orderAmount = new BigDecimal("150000");

                CouponValidationResponse expectedResponse = new CouponValidationResponse(
                                couponId, // couponId
                                userId, // userId
                                false, // usable
                                BigDecimal.ZERO, // discountAmount
                                orderAmount, // finalAmount (할인 없음)
                                "보유하지 않은 쿠폰입니다." // reason
                );

                when(couponService.validateAndCalculateDiscount(userId, couponId, orderAmount))
                                .thenReturn(expectedResponse);

                // When
                CouponValidationResponse response = validateCouponUseCase.execute(userId, couponId, orderAmount);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.usable()).isFalse();
                assertThat(response.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(response.finalAmount()).isEqualByComparingTo(orderAmount);
                assertThat(response.reason()).contains("보유하지 않은");

                verify(couponService).validateAndCalculateDiscount(userId, couponId, orderAmount);
        }

        @Test
        @DisplayName("UseCase 단일 책임 검증 - execute 메서드만 존재한다")
        void UseCase_단일책임_검증() {
                // Given: UseCase 클래스 메서드 확인
                var methods = ValidateCouponUseCase.class.getDeclaredMethods();
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
        @DisplayName("ReadOnly 트랜잭션 확인 - @Transactional(readOnly=true)가 적용되어 있다")
        void ReadOnly트랜잭션_확인() {
                // Given: UseCase 클래스
                Class<ValidateCouponUseCase> clazz = ValidateCouponUseCase.class;

                // Then: @Transactional(readOnly=true) 어노테이션이 적용되어 있어야 함
                var transactional = clazz.getAnnotation(
                                org.springframework.transaction.annotation.Transactional.class);

                assertThat(transactional).isNotNull();
                assertThat(transactional.readOnly()).isTrue();
        }

        @Test
        @DisplayName("UseCase 어노테이션 확인 - @UseCase가 적용되어 있다")
        void UseCase_어노테이션_확인() {
                // Given: UseCase 클래스
                Class<ValidateCouponUseCase> clazz = ValidateCouponUseCase.class;

                // Then: @UseCase 어노테이션이 적용되어 있어야 함
                boolean hasUseCase = clazz.isAnnotationPresent(
                                kr.hhplus.be.server.common.annotation.UseCase.class);

                assertThat(hasUseCase).isTrue();
        }

        @Test
        @DisplayName("의존성 주입 확인 - CouponService만 의존한다")
        void 의존성주입_확인() {
                // Given: UseCase 필드 확인
                var fields = ValidateCouponUseCase.class.getDeclaredFields();
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
                var executeMethod = ValidateCouponUseCase.class.getDeclaredMethod(
                                "execute", Long.class, Long.class, BigDecimal.class);

                // Then: 메서드 시그니처가 올바른지 확인
                assertThat(executeMethod.getReturnType()).isEqualTo(CouponValidationResponse.class);
                assertThat(executeMethod.getParameterTypes())
                                .containsExactly(Long.class, Long.class, BigDecimal.class);
        }

        @Test
        @DisplayName("할인 금액 계산 검증 - 정률/정액 할인이 올바르게 계산된다")
        void 할인금액계산_검증() {
                // Given: 정률 할인 (10%)
                Long userId = 1L;
                Long couponId = 1L;
                BigDecimal orderAmount = new BigDecimal("200000");

                CouponValidationResponse percentageResponse = new CouponValidationResponse(
                                couponId, userId, true,
                                new BigDecimal("20000"), // 200,000 * 10% = 20,000
                                new BigDecimal("180000"), // 200,000 - 20,000 = 180,000
                                null);

                when(couponService.validateAndCalculateDiscount(userId, couponId, orderAmount))
                                .thenReturn(percentageResponse);

                // When
                CouponValidationResponse response = validateCouponUseCase.execute(userId, couponId, orderAmount);

                // Then: 정률 할인이 올바르게 적용되었는지 검증
                assertThat(response.discountAmount()).isEqualByComparingTo(new BigDecimal("20000"));
                assertThat(response.finalAmount()).isEqualByComparingTo(new BigDecimal("180000"));

                // Given: 정액 할인 (5000원)
                Long fixedCouponId = 2L;
                CouponValidationResponse fixedResponse = new CouponValidationResponse(
                                fixedCouponId, userId, true,
                                new BigDecimal("5000"), // 정액 5000원 할인
                                new BigDecimal("195000"), // 200,000 - 5,000 = 195,000
                                null);

                when(couponService.validateAndCalculateDiscount(userId, fixedCouponId, orderAmount))
                                .thenReturn(fixedResponse);

                // When
                CouponValidationResponse fixedResult = validateCouponUseCase.execute(userId, fixedCouponId,
                                orderAmount);

                // Then: 정액 할인이 올바르게 적용되었는지 검증
                assertThat(fixedResult.discountAmount()).isEqualByComparingTo(new BigDecimal("5000"));
                assertThat(fixedResult.finalAmount()).isEqualByComparingTo(new BigDecimal("195000"));

                verify(couponService).validateAndCalculateDiscount(userId, couponId, orderAmount);
                verify(couponService).validateAndCalculateDiscount(userId, fixedCouponId, orderAmount);
        }
}