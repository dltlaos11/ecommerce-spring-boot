package kr.hhplus.be.server.common.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring ApplicationEvent 기반 이벤트 발행 구현체
 * 
 * 현재 구현 (학습 목적):
 * - Spring의 ApplicationEventPublisher 활용
 * - @TransactionalEventListener(AFTER_COMMIT) 사용
 * 
 * 미래 구현으로 KafkaEventPublisher로 교체 가능
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publishEvent(DomainEvent event) {
        log.info("🎯 이벤트 발행: type={}, eventId={}, aggregateId={}",
                event.getEventType(), event.getEventId(), event.getAggregateId());

        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishEventAfterCommit(DomainEvent event) {
        log.info("🔄 트랜잭션 커밋 후 이벤트 발행 예약: type={}, eventId={}",
                event.getEventType(), event.getEventId());

        // Spring에서는 publishEvent가 이미 트랜잭션 인식하므로 동일하게 처리
        // 실제 AFTER_COMMIT 처리는 @TransactionalEventListener에서 담당
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    @Async
    public void publishEventAsync(DomainEvent event) {
        log.info("🚀 비동기 이벤트 발행: type={}, eventId={}",
                event.getEventType(), event.getEventId());

        applicationEventPublisher.publishEvent(event);
    }
}