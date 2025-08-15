package kr.hhplus.be.server.coupon.application;


import kr.hhplus.be.server.common.annotation.UseCase;
import kr.hhplus.be.server.common.lock.DistributedLock;
import kr.hhplus.be.server.common.lock.Lockable;
import kr.hhplus.be.server.coupon.dto.IssuedCouponResponse;
import kr.hhplus.be.server.coupon.lock.CouponIssueLock;
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
public class IssueCouponUseCase implements Lockable {

    private final CouponService couponService;
    
    private Long currentCouponId; // 현재 처리 중인 쿠폰 ID
    private Long currentUserId;   // 현재 처리 중인 사용자 ID

    /**
     * 쿠폰 발급 유스케이스 실행
     */
    @DistributedLock(key = "ecommerce:coupon:issue", waitTime = 3000, leaseTime = 5000)
    public IssuedCouponResponse execute(Long couponId, Long userId) {
        this.currentCouponId = couponId;
        this.currentUserId = userId;
        
        log.info("쿠폰 발급 유스케이스 실행: couponId = {}, userId = {}", couponId, userId);

        IssuedCouponResponse response = couponService.issueCoupon(couponId, userId);

        return response;
    }
    
    @Override
    public String getLockKey() {
        if (currentCouponId != null && currentUserId != null) {
            return new CouponIssueLock(currentCouponId, currentUserId).getLockKey();
        }
        return "ecommerce:coupon:issue:default";
    }
}