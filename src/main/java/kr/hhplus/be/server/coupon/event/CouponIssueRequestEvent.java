package kr.hhplus.be.server.coupon.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 쿠폰 발급 요청 이벤트
 * Redis 큐를 통한 비동기 처리를 위한 이벤트
 */
@Getter
@AllArgsConstructor
public class CouponIssueRequestEvent {
    
    private final String requestId;     // UUID 기반 요청 추적 ID
    private final Long userId;          // 사용자 ID
    private final Long couponId;        // 쿠폰 ID
    private final LocalDateTime requestedAt; // 요청 시간
    
    public static CouponIssueRequestEvent of(String requestId, Long userId, Long couponId) {
        return new CouponIssueRequestEvent(requestId, userId, couponId, LocalDateTime.now());
    }
}