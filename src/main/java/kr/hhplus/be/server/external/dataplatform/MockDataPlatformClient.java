package kr.hhplus.be.server.external.dataplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.order.event.OrderDataPlatformEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockDataPlatformClient implements DataPlatformClient {
    
    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    
    private static final double FAILURE_RATE = 0.02;
    
    @Override
    public DataPlatformResponse sendOrderData(OrderDataPlatformEvent event) {
        try {
            // 네트워크 지연 시뮬레이션
            Thread.sleep(100 + random.nextInt(200));
            
            // 2% 확률 실패 시뮬레이션
            if (random.nextDouble() < FAILURE_RATE) {
                log.warn("🚨 데이터 플랫폼 전송 실패 시뮬레이션 - orderId: {}", event.orderId());
                return DataPlatformResponse.failure("외부 데이터 플랫폼 일시적 장애");
            }
            
            String messageId = UUID.randomUUID().toString();
            log.info("📊 데이터 플랫폼 전송 성공 - messageId: {}, orderId: {}, totalAmount: {}", 
                messageId, event.orderId(), event.totalAmount());
            
            return DataPlatformResponse.success(messageId);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return DataPlatformResponse.failure("전송 중 인터럽트 발생");
        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 실패 - orderId: {}", event.orderId(), e);
            return DataPlatformResponse.failure("예상치 못한 오류: " + e.getMessage());
        }
    }
    
    @Override
    public boolean isHealthy() {
        return random.nextDouble() > 0.05; // 95% 정상
    }
}