package kr.hhplus.be.server.common.event;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import kr.hhplus.be.server.common.config.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka 기반 이벤트 발행 구현체
 * 
 * 핵심 원칙:
 * - 트랜잭션 커밋 후에만 Kafka로 메시지 발행 (절대 원칙)
 * - 멱등성 보장을 위한 이벤트 ID 포함
 * - 기존 EventPublisher 인터페이스 동일 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Primary
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publishEvent(DomainEvent event) {
        log.info("🎯 Kafka 즉시 이벤트 발행: type={}, eventId={}, aggregateId={}",
                event.getEventType(), event.getEventId(), event.getAggregateId());

        sendToKafka(event);
    }

    @Override
    public void publishEventAfterCommit(DomainEvent event) {
        log.info("🔄 Kafka 트랜잭션 커밋 후 이벤트 발행 예약: type={}, eventId={}",
                event.getEventType(), event.getEventId());

        // Spring의 ApplicationEventPublisher를 통해 내부 이벤트 발행
        // TransactionalKafkaEventHandler가 이를 감지하여 커밋 후 처리
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    @Async
    public void publishEventAsync(DomainEvent event) {
        log.info("🚀 Kafka 비동기 이벤트 발행: type={}, eventId={}",
                event.getEventType(), event.getEventId());

        sendToKafkaAsync(event);
    }

    /**
     * 실제 Kafka 전송 로직 (발송 보장 개선)
     *
     * 피드백 반영:
     * - KafkaTemplate.send는 CompletableFuture를 리턴하므로 발송을 보장하지 않음
     * - 동기 전송 또는 콜백을 통한 결과 처리로 발송 보장
     */
    private void sendToKafka(DomainEvent event) {
        try {
            String topicName = generateTopicName(event.getEventType());
            String partitionKey = generatePartitionKey(event);

            // Kafka 메시지 구성 (메타데이터 포함)
            Message<DomainEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, topicName)
                    .setHeader(KafkaHeaders.KEY, partitionKey)
                    .setHeader("eventId", event.getEventId())
                    .setHeader("eventType", event.getEventType())
                    .setHeader("aggregateId", event.getAggregateId())
                    .setHeader("occurredOn", event.getOccurredAt().toString())
                    .build();

            // 동기 전송으로 발송 보장 (타임아웃 5초)
            SendResult<String, Object> result = kafkaTemplate.send(message)
                    .get(5, TimeUnit.SECONDS);

            log.info("✅ Kafka 발행 성공: topic={}, partition={}, offset={}, key={}, eventId={}",
                    topicName,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    partitionKey,
                    event.getEventId());

        } catch (Exception e) {
            log.error("💥 Kafka 발행 실패: eventType={}, eventId={}",
                    event.getEventType(), event.getEventId(), e);

            // 발송 실패 시 예외 발생 (상위 서비스에서 처리)
            throw new RuntimeException("Kafka 이벤트 발행 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 비동기 Kafka 전송 로직 (CompletableFuture 기반 결과 처리)
     */
    private void sendToKafkaAsync(DomainEvent event) {
        try {
            String topicName = generateTopicName(event.getEventType());
            String partitionKey = generatePartitionKey(event);

            // Kafka 메시지 구성 (메타데이터 포함)
            Message<DomainEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, topicName)
                    .setHeader(KafkaHeaders.KEY, partitionKey)
                    .setHeader("eventId", event.getEventId())
                    .setHeader("eventType", event.getEventType())
                    .setHeader("aggregateId", event.getAggregateId())
                    .setHeader("occurredOn", event.getOccurredAt().toString())
                    .build();

            // CompletableFuture를 통한 비동기 결과 처리
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(message);

            future.whenComplete((result, failure) -> {
                if (failure != null) {
                    log.error("💥 Kafka 비동기 발행 실패: eventType={}, eventId={}",
                            event.getEventType(), event.getEventId(), failure);
                } else {
                    log.info("✅ Kafka 비동기 발행 성공: topic={}, partition={}, offset={}, key={}, eventId={}",
                            topicName,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            partitionKey,
                            event.getEventId());
                }
            });

        } catch (Exception e) {
            log.error("💥 Kafka 비동기 발행 준비 실패: eventType={}, eventId={}",
                    event.getEventType(), event.getEventId(), e);
        }
    }

    /**
     * 이벤트 타입에 따른 토픽명 생성
     */
    private String generateTopicName(String eventType) {
        return switch (eventType) {
            case "ORDER_COMPLETED", "ORDER_COMPLETED_FOR_DATA_PLATFORM" -> KafkaTopics.ORDER_COMPLETED;
            case "COUPON_ISSUED" -> KafkaTopics.COUPON_ISSUE;
            case "USER_ACTIVITY" -> KafkaTopics.USER_ACTIVITY;
            case "BALANCE_CHARGED" -> KafkaTopics.BALANCE_ACTIVITY;
            case "PRODUCT_STOCK_CHANGED" -> KafkaTopics.PRODUCT_ACTIVITY;
            default -> KafkaTopics.GENERAL_EVENTS;
        };
    }

    /**
     * 파티션 키 생성 전략
     * 
     * 순서 보장이 필요한 경우: aggregateId 사용
     * 단순 로드밸런싱: null (라운드로빈)
     */
    private String generatePartitionKey(DomainEvent event) {
        return switch (event.getEventType()) {
            case "COUPON_ISSUED" ->
                // 쿠폰별로 순서 보장 (동일 쿠폰 = 동일 파티션)
                "coupon:" + event.getAggregateId();

            case "ORDER_COMPLETED", "ORDER_COMPLETED_FOR_DATA_PLATFORM" ->
                // 주문은 로드밸런싱 (순서 보장 불필요)
                null;

            case "BALANCE_CHARGED" ->
                // 사용자별로 순서 보장
                "user:" + event.getAggregateId();

            default -> null;
        };
    }

}