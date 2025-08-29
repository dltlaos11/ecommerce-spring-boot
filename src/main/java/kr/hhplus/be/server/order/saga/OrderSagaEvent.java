package kr.hhplus.be.server.order.saga;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import kr.hhplus.be.server.order.dto.OrderItemRequest;

/**
 * 주문 Saga 패턴을 위한 이벤트 정의
 * 
 * 코레오그래피 방식의 Saga 패턴을 구현하기 위한 이벤트들
 * 각 서비스는 이벤트를 발행하고 구독하여 분산 트랜잭션을 처리
 */
public class OrderSagaEvent {
    
    public static class OrderInitiated {
        private final String sagaId;
        private final Long userId;
        private final List<OrderItemRequest> items;
        private final Long couponId;
        private final BigDecimal totalAmount;
        private final LocalDateTime timestamp;
        
        public OrderInitiated(Long userId, List<OrderItemRequest> items, Long couponId, BigDecimal totalAmount) {
            this.sagaId = UUID.randomUUID().toString();
            this.userId = userId;
            this.items = items;
            this.couponId = couponId;
            this.totalAmount = totalAmount;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getSagaId() { return sagaId; }
        public Long getUserId() { return userId; }
        public List<OrderItemRequest> getItems() { return items; }
        public Long getCouponId() { return couponId; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    public static class StockValidated {
        private final String sagaId;
        private final Long userId;
        private final List<OrderItemRequest> items;
        private final BigDecimal totalAmount;
        private final LocalDateTime timestamp;
        
        public StockValidated(String sagaId, Long userId, List<OrderItemRequest> items, BigDecimal totalAmount) {
            this.sagaId = sagaId;
            this.userId = userId;
            this.items = items;
            this.totalAmount = totalAmount;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getSagaId() { return sagaId; }
        public Long getUserId() { return userId; }
        public List<OrderItemRequest> getItems() { return items; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    public static class PaymentProcessed {
        private final String sagaId;
        private final Long userId;
        private final BigDecimal finalAmount;
        private final String paymentId;
        private final LocalDateTime timestamp;
        
        public PaymentProcessed(String sagaId, Long userId, BigDecimal finalAmount, String paymentId) {
            this.sagaId = sagaId;
            this.userId = userId;
            this.finalAmount = finalAmount;
            this.paymentId = paymentId;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getSagaId() { return sagaId; }
        public Long getUserId() { return userId; }
        public BigDecimal getFinalAmount() { return finalAmount; }
        public String getPaymentId() { return paymentId; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    public static class StockReserved {
        private final String sagaId;
        private final Long userId;
        private final List<OrderItemRequest> items;
        private final LocalDateTime timestamp;
        
        public StockReserved(String sagaId, Long userId, List<OrderItemRequest> items) {
            this.sagaId = sagaId;
            this.userId = userId;
            this.items = items;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getSagaId() { return sagaId; }
        public Long getUserId() { return userId; }
        public List<OrderItemRequest> getItems() { return items; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    public static class OrderCreated {
        private final String sagaId;
        private final Long orderId;
        private final String orderNumber;
        private final Long userId;
        private final BigDecimal finalAmount;
        private final LocalDateTime timestamp;
        
        public OrderCreated(String sagaId, Long orderId, String orderNumber, Long userId, BigDecimal finalAmount) {
            this.sagaId = sagaId;
            this.orderId = orderId;
            this.orderNumber = orderNumber;
            this.userId = userId;
            this.finalAmount = finalAmount;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getSagaId() { return sagaId; }
        public Long getOrderId() { return orderId; }
        public String getOrderNumber() { return orderNumber; }
        public Long getUserId() { return userId; }
        public BigDecimal getFinalAmount() { return finalAmount; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    // 보상 트랜잭션 이벤트들
    public static class OrderFailed {
        private final String sagaId;
        private final Long userId;
        private final String reason;
        private final String failureStep;
        private final LocalDateTime timestamp;
        
        public OrderFailed(String sagaId, Long userId, String reason, String failureStep) {
            this.sagaId = sagaId;
            this.userId = userId;
            this.reason = reason;
            this.failureStep = failureStep;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getSagaId() { return sagaId; }
        public Long getUserId() { return userId; }
        public String getReason() { return reason; }
        public String getFailureStep() { return failureStep; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    public static class PaymentCompensated {
        private final String sagaId;
        private final Long userId;
        private final BigDecimal amount;
        private final String originalPaymentId;
        private final LocalDateTime timestamp;
        
        public PaymentCompensated(String sagaId, Long userId, BigDecimal amount, String originalPaymentId) {
            this.sagaId = sagaId;
            this.userId = userId;
            this.amount = amount;
            this.originalPaymentId = originalPaymentId;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getSagaId() { return sagaId; }
        public Long getUserId() { return userId; }
        public BigDecimal getAmount() { return amount; }
        public String getOriginalPaymentId() { return originalPaymentId; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    public static class StockCompensated {
        private final String sagaId;
        private final List<OrderItemRequest> items;
        private final LocalDateTime timestamp;
        
        public StockCompensated(String sagaId, List<OrderItemRequest> items) {
            this.sagaId = sagaId;
            this.items = items;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getSagaId() { return sagaId; }
        public List<OrderItemRequest> getItems() { return items; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}