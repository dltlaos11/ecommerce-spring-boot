package kr.hhplus.be.server.external.dataplatform;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.hhplus.be.server.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockDataPlatformClient implements DataPlatformClient {

    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    private static final double FAILURE_RATE = 0.02;

    @Override
    public DataPlatformResponse sendEventData(DomainEvent event) {
        try {
            // 네트워크 지연 시뮬레이션
            Thread.sleep(100 + random.nextInt(200));

            // 2% 확률 실패 시뮬레이션
            if (random.nextDouble() < FAILURE_RATE) {
                log.warn("🚨 데이터 플랫폼 전송 실패 시뮬레이션 - eventType: {}, eventId: {}",
                        event.getEventType(), event.getEventId());
                return DataPlatformResponse.failure("외부 데이터 플랫폼 일시적 장애");
            }

            String messageId = UUID.randomUUID().toString();
            log.info("📊 데이터 플랫폼 전송 성공 - messageId: {}, eventType: {}, eventId: {}, aggregateId: {}",
                    messageId, event.getEventType(), event.getEventId(), event.getAggregateId());

            return DataPlatformResponse.success(messageId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return DataPlatformResponse.failure("전송 중 인터럽트 발생");
        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 실패 - eventType: {}, eventId: {}",
                    event.getEventType(), event.getEventId(), e);
            return DataPlatformResponse.failure("예상치 못한 오류: " + e.getMessage());
        }
    }

    @Override
    public DataPlatformResponse sendEventData(String eventType, Object eventData) {
        try {
            Thread.sleep(100 + random.nextInt(200));

            if (random.nextDouble() < FAILURE_RATE) {
                log.warn("🚨 데이터 플랫폼 전송 실패 시뮬레이션 - eventType: {}", eventType);
                return DataPlatformResponse.failure("외부 데이터 플랫폼 일시적 장애");
            }

            String messageId = UUID.randomUUID().toString();
            String jsonData = objectMapper.writeValueAsString(eventData);

            log.info("📊 데이터 플랫폼 전송 성공 - messageId: {}, eventType: {}, dataSize: {} bytes",
                    messageId, eventType, jsonData.length());

            return DataPlatformResponse.success(messageId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return DataPlatformResponse.failure("전송 중 인터럽트 발생");
        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 실패 - eventType: {}", eventType, e);
            return DataPlatformResponse.failure("예상치 못한 오류: " + e.getMessage());
        }
    }

    @Override
    public DataPlatformResponse sendBatchEventData(List<DomainEvent> events) {
        try {
            Thread.sleep(200 + random.nextInt(300)); // 배치는 좀 더 오래 걸림

            if (random.nextDouble() < FAILURE_RATE) {
                log.warn("🚨 배치 데이터 플랫폼 전송 실패 시뮬레이션 - eventCount: {}", events.size());
                return DataPlatformResponse.failure("배치 전송 중 외부 플랫폼 장애");
            }

            String batchId = UUID.randomUUID().toString();
            log.info("📊 배치 데이터 플랫폼 전송 성공 - batchId: {}, eventCount: {}",
                    batchId, events.size());

            return DataPlatformResponse.success(batchId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return DataPlatformResponse.failure("배치 전송 중 인터럽트 발생");
        } catch (Exception e) {
            log.error("배치 데이터 플랫폼 전송 실패 - eventCount: {}", events.size(), e);
            return DataPlatformResponse.failure("배치 전송 실패: " + e.getMessage());
        }
    }

    @Override
    public boolean isHealthy() {
        return random.nextDouble() > 0.05; // 95% 정상
    }
}