package kr.hhplus.be.server.common.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 트랜잭션 커밋 후 Kafka 이벤트 처리를 담당하는 핸들러
 * 
 * KafkaEventPublisher와 분리하여 프록시 문제 해결
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionalKafkaEventHandler {

    private final EventPublisher eventPublisher;

    /**
     * 트랜잭션 커밋 후 실제 Kafka로 발행
     * 
     * 커밋이 안됐는데 이미 메시지를 발행하면, 구독 서비스는 id를 보고
     * 발행 서비스에 데이터 요청 -> 없음 -> 실패
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleTransactionalEvent(DomainEvent event) {
        log.info("📡 트랜잭션 커밋 완료 후 Kafka 발행: type={}, eventId={}",
                event.getEventType(), event.getEventId());

        // EventPublisher의 즉시 발행 메서드 사용 (실제로는 KafkaEventPublisher가 동작)
        eventPublisher.publishEvent(event);
    }
}