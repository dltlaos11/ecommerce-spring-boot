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
        // 쿠폰 발급은 쿠폰별로 전체 발급 수량을 제어해야 하므로 쿠폰 ID만 사용
        return "ecommerce:coupon:issue:" + couponId;
    }
}