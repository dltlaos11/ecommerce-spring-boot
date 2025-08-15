package kr.hhplus.be.server.coupon.lock;

import kr.hhplus.be.server.common.lock.Lockable;
import lombok.RequiredArgsConstructor;

/**
 * 쿠폰 발급을 위한 분산락 키 생성
 */
@RequiredArgsConstructor
public class CouponIssueLock implements Lockable {
    
    private final Long couponId;
    private final Long userId;
    
    @Override
    public String getLockKey() {
        return "ecommerce:coupon:issue:" + couponId + ":" + userId;
    }
}