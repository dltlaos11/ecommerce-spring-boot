package kr.hhplus.be.server.order.event;

import kr.hhplus.be.server.external.dataplatform.DataPlatformClient;
import kr.hhplus.be.server.external.dataplatform.DataPlatformResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataPlatformEventHandler {
    
    private final DataPlatformClient dataPlatformClient;
    
    @EventListener
    @Async
    public void handleOrderDataPlatformEvent(OrderDataPlatformEvent event) {
        log.info("데이터 플랫폼 이벤트 수신: orderId={}", event.orderId());
        
        try {
            DataPlatformResponse response = dataPlatformClient.sendOrderData(event);
            
            if (response.success()) {
                log.info("데이터 플랫폼 전송 완료: orderId={}, messageId={}", 
                    event.orderId(), response.messageId());
            } else {
                log.error("데이터 플랫폼 전송 실패: orderId={}, reason={}", 
                    event.orderId(), response.message());
            }
            
        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 예외: orderId={}", event.orderId(), e);
        }
    }
}