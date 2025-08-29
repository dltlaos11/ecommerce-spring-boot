package kr.hhplus.be.server.common.event;

/**
 * 이벤트 발행 추상화 인터페이스
 * 
 * 멘토링 핵심 인사이트:
 * - ApplicationEvent에서 Kafka로 "갈아끼우기" 가능한 구조
 * - 구현체만 교체하면 되는 추상화
 */
public interface EventPublisher {
    
    /**
     * 도메인 이벤트 발행
     * 
     * @param event 발행할 도메인 이벤트
     */
    void publishEvent(DomainEvent event);
    
    /**
     * 트랜잭션 커밋 후 이벤트 발행 (Spring 전용)
     * 
     * @param event 발행할 도메인 이벤트
     */
    void publishEventAfterCommit(DomainEvent event);
}