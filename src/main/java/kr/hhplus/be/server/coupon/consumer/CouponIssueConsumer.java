package kr.hhplus.be.server.coupon.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.coupon.event.CouponIssueEvent;
import kr.hhplus.be.server.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 쿠폰 발급 Kafka Consumer
 * 
 * - 멱등성 보장: 중복 발급 체크 (쿠폰ID + 유저ID)
 * - DB 영속화: 안정적인 쿠폰 발급 완료 처리
 * - 에러 처리: 실패 시 재시도 및 DLQ 처리
 * 
 * -> Consumer에서 비즈니스 멱등성을 직접 구현해야 함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueConsumer {

    private final UserCouponRepository userCouponRepository;

    /**
     * 쿠폰 발급 이벤트 처리
     * 
     * 파티션 전략:
     * - 쿠폰ID를 키로 사용하여 동일 쿠폰은 같은 파티션으로
     * - 단일 쿠폰의 순서 보장 (선착순 보장)
     */
    @KafkaListener(topics = "${kafka.topics.coupon-issue}", groupId = "${kafka.consumer-groups.coupon-issue}", concurrency = "${app.kafka.listeners.coupon-issue.concurrency:3}")
    @Transactional
    public void handleCouponIssue(
            @Payload CouponIssueEvent event,
            @Header(name = KafkaHeaders.RECEIVED_KEY) String key,
            @Header(name = KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(name = KafkaHeaders.OFFSET) long offset,
            @Header(name = "eventId", required = false) String eventId) {

        log.info("📥 쿠폰 발급 이벤트 수신: " +
                "eventId={}, couponId={}, userId={}, partition={}, offset={}",
                event.eventId(), event.couponId(), event.userId(), partition, offset);

        try {
            // 1단계: 멱등성 보장 - 중복 발급 체크
            if (isAlreadyIssued(event.couponId(), event.userId())) {
                log.info("⚠️ 이미 발급된 쿠폰 스킵: couponId={}, userId={}, eventId={}",
                        event.couponId(), event.userId(), event.eventId());
                return; // 자동 커밋으로 처리됨
            }

            // 2단계: DB에 쿠폰 발급 정보 영속화
            UserCoupon userCoupon = new UserCoupon(event.userId(), event.couponId());
            UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

            log.info("✅ 쿠폰 발급 완료: " +
                    "userCouponId={}, couponId={}, userId={}, eventId={}",
                    savedUserCoupon.getId(), event.couponId(), event.userId(), event.eventId());

            // 3단계: 처리 완료 로그 (모니터링용)
            logProcessingMetrics(event, partition, offset);

            // 성공 시 자동 커밋으로 처리 (Spring Boot 기본 방식)

        } catch (Exception e) {
            log.error("💥 쿠폰 발급 처리 실패: " +
                    "eventId={}, couponId={}, userId={}, partition={}, offset={}",
                    event.eventId(), event.couponId(), event.userId(), partition, offset, e);

            // Spring Kafka의 기본 재시도 메커니즘에 의해 자동 재시도
            // 최종 실패 시 DLQ(Dead Letter Queue)로 이동 예정
            throw new RuntimeException("쿠폰 발급 처리 실패", e);
        }
    }

    /**
     * 중복 발급 체크 (멱등성 보장)
     * 
     * 쿠폰ID + 유저ID로 중복 방지
     * 이미 발급받은 사용자면 '있어'라고 단순 응답
     */
    private boolean isAlreadyIssued(Long couponId, Long userId) {
        return userCouponRepository.existsByUserIdAndCouponId(userId, couponId);
    }

    /**
     * Consumer 성능 모니터링 로그
     * 
     * "Consumer Lag 모니터링 필수"
     */
    private void logProcessingMetrics(CouponIssueEvent event, int partition, long offset) {
        long processingDelay = System.currentTimeMillis() -
                event.occurredAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

        if (processingDelay > 1000) { // 1초 이상 지연 시 경고
            log.warn("🐌 Consumer 지연 감지: {}ms, partition={}, offset={}, eventId={}",
                    processingDelay, partition, offset, event.eventId());
        }

        log.debug("📊 처리 완료: partition={}, offset={}, delay={}ms",
                partition, offset, processingDelay);
    }
}