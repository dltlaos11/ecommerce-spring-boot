package kr.hhplus.be.server.order.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import kr.hhplus.be.server.external.dataplatform.DataPlatformClient;
import kr.hhplus.be.server.external.dataplatform.DataPlatformResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataPlatformEventHandler {

    private final DataPlatformClient dataPlatformClient;

    /**
     * 주문 완료 후 데이터 플랫폼 이벤트 처리
     * 
     * - 트랜잭션 커밋 완료 후에만 이벤트 발행
     * - 커밋이 안됐는데 이미 메시지를 발행하면, 구독 서비스는 id를 보고 발행 서비스에 데이터 요청을 다시 진행 -> '없는데?' 이렇게
     * 되는 경우가 발생
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleOrderDataPlatformEvent(OrderDataPlatformEvent event) {
        log.info("📡 데이터 플랫폼 이벤트 수신 (트랜잭션 커밋 후): orderId={}", event.orderId());

        try {
            // 새로운 범용 인터페이스 사용
            DataPlatformResponse response = dataPlatformClient.sendEventData(
                    "ORDER_COMPLETED",
                    event);

            if (response.success()) {
                log.info("✅ 데이터 플랫폼 전송 완료: orderId={}, messageId={}",
                        event.orderId(), response.messageId());
            } else {
                log.error("❌ 데이터 플랫폼 전송 실패: orderId={}, reason={}",
                        event.orderId(), response.message());

                // DLQ(Dead Letter Queue)로 처리 예정
                scheduleRetry(event);
            }

        } catch (Exception e) {
            log.error("💥 데이터 플랫폼 전송 예외: orderId={}", event.orderId(), e);
            scheduleRetry(event);
        }
    }

    /**
     * 실패한 이벤트 재시도 스케줄링
     * Kafka 기반 DLQ로 개선 예정
     */
    private void scheduleRetry(OrderDataPlatformEvent event) {
        log.info("🔄 데이터 플랫폼 전송 재시도 스케줄링: orderId={}", event.orderId());
        // 현재는 로깅만, Kafka 재시도 로직 구현
    }
}