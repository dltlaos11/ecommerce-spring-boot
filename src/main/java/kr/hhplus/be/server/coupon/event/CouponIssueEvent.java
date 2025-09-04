package kr.hhplus.be.server.coupon.event;

import java.time.LocalDateTime;

import kr.hhplus.be.server.common.event.DomainEvent;
import lombok.Builder;

/**
 * 쿠폰 발급 이벤트
 * 
 * - Redis로 빠른 선착순 제어 완료 후 Kafka로 안정적 발급 처리
 * - 멱등성 보장을 위한 eventId 포함
 * - 쿠폰별 순서 보장을 위한 쿠폰ID 파티션 키 사용
 */
@Builder
public record CouponIssueEvent(
        String eventId,
        Long couponId,
        Long userId,
        String requestId,
        LocalDateTime issuedAt,
        LocalDateTime occurredAt) implements DomainEvent {

    public static CouponIssueEvent create(Long couponId, Long userId, String requestId) {
        LocalDateTime now = LocalDateTime.now();
        return CouponIssueEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .couponId(couponId)
                .userId(userId)
                .requestId(requestId)
                .issuedAt(now)
                .occurredAt(now)
                .build();
    }

    @Override
    public String getEventType() {
        return "COUPON_ISSUED";
    }

    @Override
    public String getAggregateId() {
        return couponId.toString();
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getEventId() {
        return eventId;
    }
}