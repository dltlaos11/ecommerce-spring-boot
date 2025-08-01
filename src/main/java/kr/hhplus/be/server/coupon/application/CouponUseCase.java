package kr.hhplus.be.server.coupon.application;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.common.annotation.UseCase;
import kr.hhplus.be.server.coupon.dto.AvailableCouponResponse;
import kr.hhplus.be.server.coupon.dto.CouponValidationResponse;
import kr.hhplus.be.server.coupon.dto.IssuedCouponResponse;
import kr.hhplus.be.server.coupon.dto.UserCouponResponse;
import kr.hhplus.be.server.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Application Layer - 쿠폰 비즈니스 Usecase
 * 
 * 책임:
 * - 쿠폰 관련 비즈니스 유스케이스 구현
 * - 선착순 쿠폰 발급 처리
 * - 쿠폰 검증 및 할인 계산
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponUseCase {

    private final CouponService couponService;

    /**
     * 발급 가능한 쿠폰 목록 조회 유스케이스
     */
    public List<AvailableCouponResponse> getAvailableCoupons() {
        log.debug("🎫 발급 가능한 쿠폰 목록 조회 유스케이스");
        return couponService.getAvailableCoupons();
    }

    /**
     * 쿠폰 발급 유스케이스 (선착순 처리)
     */
    @Transactional
    public IssuedCouponResponse issueCoupon(Long couponId, Long userId) {
        log.info("🎫 쿠폰 발급 유스케이스: couponId = {}, userId = {}", couponId, userId);

        IssuedCouponResponse response = couponService.issueCoupon(couponId, userId);

        log.info("✅ 쿠폰 발급 유스케이스 완료: couponId = {}, userId = {}", couponId, userId);
        return response;
    }

    /**
     * 사용자 쿠폰 목록 조회 유스케이스
     */
    public List<UserCouponResponse> getUserCoupons(Long userId) {
        log.debug("📋 사용자 쿠폰 목록 조회 유스케이스: userId = {}", userId);
        return couponService.getUserCoupons(userId);
    }

    /**
     * 사용 가능한 쿠폰 목록 조회 유스케이스
     */
    public List<UserCouponResponse> getAvailableUserCoupons(Long userId) {
        log.debug("📋 사용 가능한 쿠폰 조회 유스케이스: userId = {}", userId);
        return couponService.getAvailableUserCoupons(userId);
    }

    /**
     * 쿠폰 검증 및 할인 계산 유스케이스
     */
    public CouponValidationResponse validateAndCalculateDiscount(Long userId, Long couponId, BigDecimal orderAmount) {
        log.debug("🧮 쿠폰 검증 유스케이스: userId = {}, couponId = {}, orderAmount = {}",
                userId, couponId, orderAmount);
        return couponService.validateAndCalculateDiscount(userId, couponId, orderAmount);
    }

    /**
     * 특정 쿠폰 조회 유스케이스
     */
    public AvailableCouponResponse getCoupon(Long couponId) {
        log.debug("🔍 쿠폰 조회 유스케이스: couponId = {}", couponId);
        return couponService.getCoupon(couponId);
    }
}