package kr.hhplus.be.server.coupon.application;

import java.math.BigDecimal;

import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.common.annotation.UseCase;
import kr.hhplus.be.server.coupon.dto.CouponValidationResponse;
import kr.hhplus.be.server.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;

/**
 * 쿠폰 검증 UseCase - 단일 비즈니스 요구사항만 처리
 * 
 * 구체적인 요구사항: "시스템이 쿠폰 사용 가능 여부를 검증한다"
 * 
 * 주문 프로세스에서 쿠폰 할인 금액 계산 및 검증을 담당
 */
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ValidateCouponUseCase {

    private final CouponService couponService;

    /**
     * 쿠폰 검증 및 할인 계산 실행
     */
    public CouponValidationResponse execute(Long userId, Long couponId, BigDecimal orderAmount) {
        return couponService.validateAndCalculateDiscount(userId, couponId, orderAmount);
    }
}