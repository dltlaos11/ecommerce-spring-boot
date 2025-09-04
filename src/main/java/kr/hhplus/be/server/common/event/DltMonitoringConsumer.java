package kr.hhplus.be.server.common.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * DLT(Dead Letter Topic) 모니터링 Consumer
 * 
 * - 실패한 메시지들을 모니터링
 * - 알림 및 수동 복구를 위한 로깅
 * - 실무에서는 슬랙 알림, 대시보드 연동 등으로 확장
 */
@Slf4j
@Component
public class DltMonitoringConsumer {

    /**
     * 쿠폰 발급 실패 메시지 모니터링
     */
    @KafkaListener(topics = "coupon-issue.DLT", groupId = "dlt-monitoring-group")
    public void handleCouponIssueDlt(
            @Payload Object failedMessage,
            @Header(name = KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(name = KafkaHeaders.RECEIVED_KEY) String key,
            @Header(name = KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(name = KafkaHeaders.OFFSET) long offset) {

        log.error("🚨 쿠폰 발급 실패 메시지 감지 [DLT]:");
        log.error("  - Topic: {}", topic);
        log.error("  - Key: {}", key);
        log.error("  - Partition: {}, Offset: {}", partition, offset);
        log.error("  - Message: {}", failedMessage);

        // 실무에서는 여기에 추가 처리:
        // 1. 슬랙/이메일 알림 발송
        // 2. 모니터링 대시보드로 메트릭 전송
        // 3. 자동 복구 시도 (비즈니스 규칙에 따라)
        // 4. 데이터베이스에 실패 로그 저장

        sendAlert("쿠폰 발급 실패", failedMessage, topic, key);
    }

    /**
     * 주문 완료 실패 메시지 모니터링
     */
    @KafkaListener(topics = "order-completed.DLT", groupId = "dlt-monitoring-group")
    public void handleOrderCompletedDlt(
            @Payload Object failedMessage,
            @Header(name = KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(name = KafkaHeaders.RECEIVED_KEY) String key,
            @Header(name = KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(name = KafkaHeaders.OFFSET) long offset) {

        log.error("🚨 주문 완료 처리 실패 메시지 감지 [DLT]:");
        log.error("  - Topic: {}", topic);
        log.error("  - Key: {}", key);
        log.error("  - Partition: {}, Offset: {}", partition, offset);
        log.error("  - Message: {}", failedMessage);

        sendAlert("주문 완료 처리 실패", failedMessage, topic, key);
    }

    /**
     * 알림 발송 (Mock 구현)
     * 실무에서는 슬랙, 이메일, SMS 등으로 확장
     */
    private void sendAlert(String alertType, Object message, String topic, String key) {
        log.warn("📢 [알림] {}: topic={}, key={}, message={}",
                alertType, topic, key, message);

        // 실제 구현 예시:
        // slackService.sendAlert(alertType, message);
        // emailService.sendFailureNotification(alertType, message);
        // metricsService.incrementFailureCounter(topic);
    }
}