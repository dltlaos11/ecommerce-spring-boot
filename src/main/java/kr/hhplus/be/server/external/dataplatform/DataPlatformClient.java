package kr.hhplus.be.server.external.dataplatform;

import kr.hhplus.be.server.order.event.OrderDataPlatformEvent;

/**
 * 데이터 플랫폼 클라이언트 인터페이스
 * 
 * 실제 구현에서는 HTTP 클라이언트나 메시징을 통해 외부 데이터 플랫폼과 연동
 */
public interface DataPlatformClient {
    
    /**
     * 주문 정보를 데이터 플랫폼으로 전송
     * 
     * @param event 주문 완료 이벤트 데이터
     * @return 전송 결과
     */
    DataPlatformResponse sendOrderData(OrderDataPlatformEvent event);
    
    /**
     * 데이터 플랫폼 연결 상태 확인
     * 
     * @return 연결 상태
     */
    boolean isHealthy();
}