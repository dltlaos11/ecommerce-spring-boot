package kr.hhplus.be.server.coupon.application;

import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.common.annotation.UseCase;
import kr.hhplus.be.server.coupon.dto.IssuedCouponResponse;
import kr.hhplus.be.server.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 쿠폰 발급 UseCase - 단일 비즈니스 요구사항만 처리
 * 
 * 하나의 구체적인 요구사항: "사용자가 선착순 쿠폰을 발급받는다"
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class IssueCouponUseCase {

    private final CouponService couponService;

    /**
     * 쿠폰 발급 유스케이스 실행
     */
    public IssuedCouponResponse execute(Long couponId, Long userId) {
        log.info("쿠폰 발급 유스케이스 실행: couponId = {}, userId = {}", couponId, userId);

        IssuedCouponResponse response = couponService.issueCoupon(couponId, userId);

        return response;
    }
}