package kr.hhplus.be.server.common.event;

/**
 * 이벤트 발행 추상화 인터페이스
 * 
 * 멘토링 핵심 인사이트:
 * - ApplicationEvent에서 Kafka로 "갈아끼우기" 가능한 구조
 * - 구현체만 교체하면 되는 추상화
 * 
 * 피드백 반영:
 * - publishEventAfterCommit를 범용적으로 개선
 * - Kafka 환경에서도 동일한 시맨틱 보장
 */
public interface EventPublisher {
    
    /**
     * 즉시 이벤트 발행
     * 현재 트랜잭션과 관계없이 즉시 발행
     * 
     * @param event 발행할 도메인 이벤트
     */
    void publishEvent(DomainEvent event);
    
    /**
     * 안전한 이벤트 발행 (커밋 후)
     * 
     * Spring: 트랜잭션 커밋 후 발행
     * Kafka: Producer 트랜잭션 커밋 후 발행
     * 
     * @param event 발행할 도메인 이벤트
     */
    void publishEventAfterCommit(DomainEvent event);
    
    /**
     * 비동기 이벤트 발행
     * 발행 후 즉시 리턴, 백그라운드에서 처리
     * 
     * @param event 발행할 도메인 이벤트
     */
    void publishEventAsync(DomainEvent event);
}