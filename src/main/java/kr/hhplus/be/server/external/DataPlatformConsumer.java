package kr.hhplus.be.server.external;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import kr.hhplus.be.server.order.event.OrderDataPlatformEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 외부 데이터 플랫폼 Mock Consumer
 * 
 * - 주문 완료 이벤트를 Kafka로부터 수신
 * - 외부 데이터 플랫폼으로의 데이터 전송 시뮬레이션
 * - Consumer 멱등성 보장 및 에러 처리
 */
@Slf4j
@Component
public class DataPlatformConsumer {

    /**
     * 주문 완료 이벤트 처리
     * 
     * - Consumer는 반드시 멱등성을 보장해야 함
     * - 처리 실패 시 자동 재시도 (Spring Kafka 기본 설정)
     * - 비즈니스 로직은 최대한 단순하게 유지
     */
    @KafkaListener(topics = "${kafka.topics.order-completed}", groupId = "${kafka.consumer-groups.data-platform}", concurrency = "${app.kafka.listeners.data-platform.concurrency:3}")
    public void handleOrderCompleted(
            @Payload OrderDataPlatformEvent event,
            @Header(name = KafkaHeaders.RECEIVED_KEY) String key,
            @Header(name = KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(name = KafkaHeaders.OFFSET) long offset,
            @Header(name = "eventId", required = false) String eventId) {
        log.info("📊 데이터 플랫폼 주문 완료 이벤트 수신: " +
                "eventId={}, orderId={}, partition={}, offset={}",
                eventId, event.orderId(), partition, offset);

        try {
            // 중복 처리 체크 (실무에서는 Redis나 DB로 중복 체크)
            if (isAlreadyProcessed(eventId, event.orderId())) {
                log.info("⚠️ 이미 처리된 이벤트 스킵: eventId={}, orderId={}",
                        eventId, event.orderId());
                return;
            }

            // 외부 데이터 플랫폼으로 데이터 전송 시뮬레이션
            sendToDataPlatform(event);

            // 처리 완료 기록 (실무에서는 처리 상태 저장)
            markAsProcessed(eventId, event.orderId());

            log.info("✅ 데이터 플랫폼 전송 완료: orderId={}, amount={}",
                    event.orderId(), event.totalAmount());

        } catch (Exception e) {
            log.error("💥 데이터 플랫폼 처리 실패: eventId={}, orderId={}",
                    eventId, event.orderId(), e);

            // Spring Kafka의 기본 재시도 메커니즘에 의해 자동 재시도
            // 최종 실패 시 DLQ(Dead Letter Queue)로 이동
            throw new RuntimeException("데이터 플랫폼 처리 실패", e);
        }
    }

    /**
     * 외부 데이터 플랫폼으로 데이터 전송 (Mock)
     */
    private void sendToDataPlatform(OrderDataPlatformEvent event) {
        // 실무에서는 HTTP API 호출이나 다른 메시징 시스템 사용
        log.info("🚀 외부 API 전송 시뮬레이션: " +
                "orderId={}, userId={}, amount={}, items={}",
                event.orderId(), event.userId(),
                event.totalAmount(), event.orderItems().size());

        // API 호출 지연 시뮬레이션 (실제로는 네트워크 I/O)
        simulateApiCall();
    }

    /**
     * API 호출 지연 시뮬레이션
     */
    private void simulateApiCall() {
        try {
            // 100-300ms 랜덤 지연으로 실제 API 호출 시뮬레이션
            Thread.sleep(100 + (long) (Math.random() * 200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("API 호출 중단됨", e);
        }
    }

    /**
     * 중복 처리 체크 (Mock 구현)
     * 실무에서는 Redis나 데이터베이스로 구현
     */
    private boolean isAlreadyProcessed(String eventId, Long orderId) {
        // Mock: 5% 확률로 중복 처리로 간주 (테스트용)
        boolean isDuplicate = Math.random() < 0.05;

        if (isDuplicate) {
            log.info("🔄 중복 처리 시뮬레이션: eventId={}, orderId={}", eventId, orderId);
        }

        return isDuplicate;
    }

    /**
     * 처리 완료 기록 (Mock 구현)
     */
    private void markAsProcessed(String eventId, Long orderId) {
        // Mock: 로그만 남김 (실무에서는 Redis/DB 저장)
        log.debug("📝 처리 완료 기록: eventId={}, orderId={}", eventId, orderId);
    }
}