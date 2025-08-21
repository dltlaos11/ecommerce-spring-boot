package kr.hhplus.be.server.order.event;

import java.time.LocalDateTime;

/**
 * 주문 완료 이벤트
 * 
 * 랭킹 시스템에서 상품별 주문 수량을 추적하기 위한 이벤트
 */
public class OrderCompletedEvent {

    private final Long orderId;
    private final Long userId;
    private final Long productId;
    private final String productName;
    private final Integer quantity;
    private final LocalDateTime orderCompletedAt;

    public OrderCompletedEvent(Long orderId, Long userId, Long productId, 
                              String productName, Integer quantity, LocalDateTime orderCompletedAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.orderCompletedAt = orderCompletedAt;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public LocalDateTime getOrderCompletedAt() {
        return orderCompletedAt;
    }

    @Override
    public String toString() {
        return "OrderCompletedEvent{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", orderCompletedAt=" + orderCompletedAt +
                '}';
    }
}