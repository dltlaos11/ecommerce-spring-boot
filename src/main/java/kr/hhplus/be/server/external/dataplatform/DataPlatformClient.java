package kr.hhplus.be.server.external.dataplatform;

import kr.hhplus.be.server.common.event.DomainEvent;

/**
 * 데이터 플랫폼 클라이언트 인터페이스
 * 
 * 피드백 반영: 구체적인 이벤트 타입에 의존하지 않는 범용 인터페이스
 * - 다양한 도메인 이벤트 타입 지원
 * - 확장 가능한 추상화 구조
 * 
 * 실제 구현에서는 HTTP 클라이언트나 메시징을 통해 외부 데이터 플랫폼과 연동
 */
public interface DataPlatformClient {
    
    /**
     * 범용 이벤트 데이터를 플랫폼으로 전송
     * 
     * @param event 도메인 이벤트 데이터 (Order, Coupon, User 등 모든 타입 지원)
     * @return 전송 결과
     */
    DataPlatformResponse sendEventData(DomainEvent event);
    
    /**
     * 특정 이벤트 타입 데이터 전송 (타입 안전성 보장)
     * 
     * @param eventType 이벤트 타입 (예: "ORDER_COMPLETED", "COUPON_ISSUED")
     * @param eventData 이벤트 데이터 (JSON 형태)
     * @return 전송 결과
     */
    DataPlatformResponse sendEventData(String eventType, Object eventData);
    
    /**
     * 데이터 플랫폼 연결 상태 확인
     * 
     * @return 연결 상태
     */
    boolean isHealthy();
    
    /**
     * 배치 이벤트 전송 (성능 최적화)
     * 
     * @param events 전송할 이벤트 리스트
     * @return 배치 전송 결과
     */
    DataPlatformResponse sendBatchEventData(java.util.List<DomainEvent> events);
}