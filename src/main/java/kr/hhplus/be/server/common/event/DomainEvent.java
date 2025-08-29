package kr.hhplus.be.server.common.event;

import java.time.LocalDateTime;

/**
 * 도메인 이벤트 기본 인터페이스
 * 
 * 카프카 확장을 고려한 추상화 설계:
 * - 모든 도메인 이벤트는 이 인터페이스를 구현
 * - 버전 관리 및 직렬화를 위한 기본 메타데이터 제공
 */
public interface DomainEvent {
    
    /**
     * 이벤트의 고유 식별자
     */
    String getEventId();
    
    /**
     * 이벤트 타입 (카프카 토픽 결정에 사용)
     */
    String getEventType();
    
    /**
     * 이벤트 발생 시간
     */
    LocalDateTime getOccurredAt();
    
    /**
     * 이벤트 스키마 버전 (호환성 관리)
     */
    default String getVersion() {
        return "1.0";
    }
    
    /**
     * 이벤트의 집계 루트 ID (파티셔닝에 사용)
     */
    String getAggregateId();
}