package kr.hhplus.be.server.order.event;

import kr.hhplus.be.server.common.event.DomainEvent;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 주문 완료 후 데이터 플랫폼 전송을 위한 이벤트
 * 
 * STEP 15: Application Event 구현
 * - 트랜잭션 커밋 후 외부 데이터 플랫폼으로 주문 정보 전송
 * - 핵심 비즈니스 로직(주문 처리)과 부가 로직(데이터 전송)의 관심사 분리
 */
public record OrderDataPlatformEvent(
    String eventId,
    Long orderId,
    Long userId,
    Long totalAmount,
    String orderStatus,
    List<OrderItemData> orderItems,
    LocalDateTime occurredAt
) implements DomainEvent {
    
    public record OrderItemData(
        Long productId,
        String productName,
        Integer quantity,
        Long unitPrice,
        Long totalPrice
    ) {
        public static OrderItemData from(OrderItem orderItem) {
            return new OrderItemData(
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getQuantity(),
                orderItem.getProductPrice().longValue(),
                orderItem.getSubtotal().longValue()
            );
        }
    }
    
    public static OrderDataPlatformEvent from(Order order) {
        return new OrderDataPlatformEvent(
            UUID.randomUUID().toString(),
            order.getId(),
            order.getUserId(),
            order.getTotalAmount().longValue(),
            order.getStatus().name(),
            List.of(), // OrderItem은 별도 조회 필요
            LocalDateTime.now()
        );
    }
    
    @Override
    public String getEventId() {
        return eventId;
    }
    
    @Override
    public String getEventType() {
        return "ORDER_COMPLETED_FOR_DATA_PLATFORM";
    }
    
    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public String getAggregateId() {
        return orderId.toString();
    }
}