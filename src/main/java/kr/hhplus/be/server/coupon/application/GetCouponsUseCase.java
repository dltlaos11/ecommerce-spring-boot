package kr.hhplus.be.server.coupon.application;

import java.util.List;

import kr.hhplus.be.server.common.annotation.UseCase;
import kr.hhplus.be.server.coupon.dto.AvailableCouponResponse;
import kr.hhplus.be.server.coupon.dto.UserCouponResponse;
import kr.hhplus.be.server.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;

/**
 * 쿠폰 조회 UseCase - 조회 기능만 처리 (검증 로직 분리됨)
 * 
 * 구체적인 요구사항들:
 * - "사용자가 발급 가능한 쿠폰 목록을 조회한다"
 * - "사용자가 보유한 쿠폰 목록을 조회한다"
 * - "사용자가 특정 쿠폰을 조회한다"
 */
@UseCase
@RequiredArgsConstructor
public class GetCouponsUseCase {

    private final CouponService couponService;

    /**
     * 발급 가능한 쿠폰 목록 조회
     */
    public List<AvailableCouponResponse> executeAvailableCoupons() {
        return couponService.getAvailableCoupons();
    }

    /**
     * 특정 쿠폰 조회
     */
    public AvailableCouponResponse executeCouponQuery(Long couponId) {
        return couponService.getCoupon(couponId);
    }

    /**
     * 사용자 보유 쿠폰 목록 조회
     */
    public List<UserCouponResponse> executeUserCoupons(Long userId) {
        return couponService.getUserCoupons(userId);
    }

    /**
     * 사용 가능한 쿠폰 목록 조회
     */
    public List<UserCouponResponse> executeAvailableUserCoupons(Long userId) {
        return couponService.getAvailableUserCoupons(userId);
    }
}