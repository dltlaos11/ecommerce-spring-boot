package kr.hhplus.be.server.order.saga;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import kr.hhplus.be.server.balance.service.BalanceService;
import kr.hhplus.be.server.common.event.EventPublisher;
import kr.hhplus.be.server.coupon.service.CouponService;
import kr.hhplus.be.server.order.dto.CreateOrderRequest;
import kr.hhplus.be.server.order.dto.OrderItemRequest;
import kr.hhplus.be.server.order.dto.OrderResponse;
import kr.hhplus.be.server.order.service.OrderService;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 Saga 오케스트레이터
 * 
 * 코레오그래피 방식의 Saga 패턴을 구현
 * 각 단계의 성공/실패 이벤트를 처리하고 다음 단계를 트리거하거나 보상 트랜잭션을 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaOrchestrator {
    
    private final ProductService productService;
    private final BalanceService balanceService;
    private final CouponService couponService;
    private final OrderService orderService;
    private final EventPublisher eventPublisher;
    
    // Saga 상태를 메모리에서 추적 (실제 환경에서는 Redis나 DB 사용)
    private final Map<String, SagaState> sagaStates = new ConcurrentHashMap<>();
    
    /**
     * 1단계: 주문 개시 이벤트 처리 → 재고 검증
     */
    @EventListener
    public void handle(OrderSagaEvent.OrderInitiated event) {
        log.info("📋 [Saga-{}] 주문 개시: userId={}", event.getSagaId(), event.getUserId());
        
        try {
            // Saga 상태 저장
            sagaStates.put(event.getSagaId(), new SagaState(event));
            
            // 재고 검증 수행
            validateStock(event);
            
            // 재고 검증 성공 이벤트 발행
            eventPublisher.publishEvent(new OrderSagaEvent.StockValidated(
                event.getSagaId(),
                event.getUserId(),
                event.getItems(),
                event.getTotalAmount()
            ));
            
        } catch (Exception e) {
            log.error("❌ [Saga-{}] 재고 검증 실패: {}", event.getSagaId(), e.getMessage());
            eventPublisher.publishEvent(new OrderSagaEvent.OrderFailed(
                event.getSagaId(), event.getUserId(), e.getMessage(), "STOCK_VALIDATION"
            ));
        }
    }
    
    /**
     * 2단계: 재고 검증 완료 → 결제 처리
     */
    @EventListener
    public void handle(OrderSagaEvent.StockValidated event) {
        log.info("✅ [Saga-{}] 재고 검증 완료 → 결제 처리 시작", event.getSagaId());
        
        try {
            SagaState sagaState = sagaStates.get(event.getSagaId());
            if (sagaState == null) {
                throw new IllegalStateException("Saga 상태를 찾을 수 없습니다: " + event.getSagaId());
            }
            
            // 쿠폰 할인 적용
            BigDecimal discountAmount = BigDecimal.ZERO;
            if (sagaState.getCouponId() != null) {
                var validation = couponService.validateAndCalculateDiscount(
                    event.getUserId(), sagaState.getCouponId(), event.getTotalAmount());
                
                if (!validation.usable()) {
                    throw new IllegalArgumentException("쿠폰을 사용할 수 없습니다: " + validation.reason());
                }
                discountAmount = validation.discountAmount();
            }
            
            BigDecimal finalAmount = event.getTotalAmount().subtract(discountAmount);
            
            // 잔액 검증 및 차감
            if (!balanceService.hasEnoughBalance(event.getUserId(), finalAmount)) {
                throw new IllegalArgumentException("잔액이 부족합니다");
            }
            
            String tempOrderId = "SAGA_" + event.getSagaId();
            balanceService.deductBalance(event.getUserId(), finalAmount, tempOrderId);
            
            // 상태 업데이트
            sagaState.setDiscountAmount(discountAmount);
            sagaState.setFinalAmount(finalAmount);
            sagaState.setPaymentId(tempOrderId);
            
            // 결제 완료 이벤트 발행
            eventPublisher.publishEvent(new OrderSagaEvent.PaymentProcessed(
                event.getSagaId(), event.getUserId(), finalAmount, tempOrderId
            ));
            
        } catch (Exception e) {
            log.error("❌ [Saga-{}] 결제 처리 실패: {}", event.getSagaId(), e.getMessage());
            eventPublisher.publishEvent(new OrderSagaEvent.OrderFailed(
                event.getSagaId(), event.getUserId(), e.getMessage(), "PAYMENT_PROCESSING"
            ));
        }
    }
    
    /**
     * 3단계: 결제 완료 → 재고 차감
     */
    @EventListener
    public void handle(OrderSagaEvent.PaymentProcessed event) {
        log.info("💳 [Saga-{}] 결제 완료 → 재고 차감 시작", event.getSagaId());
        
        try {
            SagaState sagaState = sagaStates.get(event.getSagaId());
            if (sagaState == null) {
                throw new IllegalStateException("Saga 상태를 찾을 수 없습니다: " + event.getSagaId());
            }
            
            // 재고 차감 수행
            for (OrderItemRequest item : sagaState.getItems()) {
                productService.reduceStock(item.productId(), item.quantity());
            }
            
            // 쿠폰 사용 처리
            if (sagaState.getCouponId() != null) {
                couponService.useCoupon(event.getUserId(), sagaState.getCouponId(), sagaState.getTotalAmount());
            }
            
            // 재고 예약 완료 이벤트 발행
            eventPublisher.publishEvent(new OrderSagaEvent.StockReserved(
                event.getSagaId(), event.getUserId(), sagaState.getItems()
            ));
            
        } catch (Exception e) {
            log.error("❌ [Saga-{}] 재고 차감 실패: {}", event.getSagaId(), e.getMessage());
            
            // 결제 보상 처리
            eventPublisher.publishEvent(new OrderSagaEvent.PaymentCompensated(
                event.getSagaId(), event.getUserId(), event.getFinalAmount(), event.getPaymentId()
            ));
            
            eventPublisher.publishEvent(new OrderSagaEvent.OrderFailed(
                event.getSagaId(), event.getUserId(), e.getMessage(), "STOCK_REDUCTION"
            ));
        }
    }
    
    /**
     * 4단계: 재고 예약 완료 → 주문 생성
     */
    @EventListener
    public void handle(OrderSagaEvent.StockReserved event) {
        log.info("📦 [Saga-{}] 재고 예약 완료 → 주문 생성 시작", event.getSagaId());
        
        try {
            SagaState sagaState = sagaStates.get(event.getSagaId());
            if (sagaState == null) {
                throw new IllegalStateException("Saga 상태를 찾을 수 없습니다: " + event.getSagaId());
            }
            
            // 상품 정보 맵 생성
            Map<Long, ProductResponse> productInfoMap = sagaState.getItems().stream()
                .collect(java.util.stream.Collectors.toMap(
                    OrderItemRequest::productId,
                    item -> productService.getProduct(item.productId()),
                    (existing, replacement) -> existing
                ));
            
            // 주문 생성 요청 구성
            CreateOrderRequest orderRequest = new CreateOrderRequest(
                event.getUserId(), sagaState.getItems(), sagaState.getCouponId()
            );
            
            // 주문 생성
            OrderResponse orderResponse = orderService.createOrderWithProductInfo(
                orderRequest,
                sagaState.getTotalAmount(),
                sagaState.getDiscountAmount(),
                sagaState.getFinalAmount(),
                productInfoMap
            );
            
            // 주문 생성 완료 이벤트 발행
            eventPublisher.publishEvent(new OrderSagaEvent.OrderCreated(
                event.getSagaId(),
                orderResponse.id(),
                orderResponse.orderNumber(),
                event.getUserId(),
                sagaState.getFinalAmount()
            ));
            
            log.info("🎉 [Saga-{}] 주문 생성 완료: orderId={}, orderNumber={}",
                event.getSagaId(), orderResponse.id(), orderResponse.orderNumber());
            
        } catch (Exception e) {
            log.error("❌ [Saga-{}] 주문 생성 실패: {}", event.getSagaId(), e.getMessage());
            
            // 재고 보상 처리
            eventPublisher.publishEvent(new OrderSagaEvent.StockCompensated(
                event.getSagaId(), event.getItems()
            ));
            
            eventPublisher.publishEvent(new OrderSagaEvent.OrderFailed(
                event.getSagaId(), event.getUserId(), e.getMessage(), "ORDER_CREATION"
            ));
        }
    }
    
    /**
     * 최종 단계: 주문 생성 완료 → Saga 정리
     */
    @EventListener
    public void handle(OrderSagaEvent.OrderCreated event) {
        log.info("🏁 [Saga-{}] Saga 완료 → 정리 작업", event.getSagaId());
        
        // Saga 상태 정리
        sagaStates.remove(event.getSagaId());
        
        log.info("✅ [Saga-{}] 주문 Saga 성공적으로 완료", event.getSagaId());
    }
    
    // ==================== 보상 트랜잭션 처리 ====================
    
    @EventListener
    public void handle(OrderSagaEvent.PaymentCompensated event) {
        log.info("🔄 [Saga-{}] 결제 보상 처리: 잔액 환불", event.getSagaId());
        
        try {
            balanceService.refundBalance(event.getUserId(), event.getAmount(), event.getOriginalPaymentId());
            log.info("✅ [Saga-{}] 결제 보상 완료", event.getSagaId());
        } catch (Exception e) {
            log.error("❌ [Saga-{}] 결제 보상 실패: {}", event.getSagaId(), e.getMessage());
        }
    }
    
    @EventListener
    public void handle(OrderSagaEvent.StockCompensated event) {
        log.info("🔄 [Saga-{}] 재고 보상 처리: 재고 복구", event.getSagaId());
        
        try {
            // 재고 복구 (재고 증가)
            for (OrderItemRequest item : event.getItems()) {
                // 실제로는 재고 증가 메서드가 필요하지만 현재는 로깅만
                log.info("재고 복구: productId={}, quantity={}", item.productId(), item.quantity());
            }
            log.info("✅ [Saga-{}] 재고 보상 완료", event.getSagaId());
        } catch (Exception e) {
            log.error("❌ [Saga-{}] 재고 보상 실패: {}", event.getSagaId(), e.getMessage());
        }
    }
    
    @EventListener
    public void handle(OrderSagaEvent.OrderFailed event) {
        log.error("💥 [Saga-{}] 주문 Saga 실패: step={}, reason={}",
            event.getSagaId(), event.getFailureStep(), event.getReason());
        
        // Saga 상태 정리
        sagaStates.remove(event.getSagaId());
        
        // 실제 환경에서는 실패 알림, 모니터링 등 추가 처리 필요
    }
    
    // ==================== 헬퍼 메서드 ====================
    
    private void validateStock(OrderSagaEvent.OrderInitiated event) {
        for (OrderItemRequest item : event.getItems()) {
            ProductResponse product = productService.getProduct(item.productId());
            
            if (!productService.hasEnoughStock(item.productId(), item.quantity())) {
                throw new IllegalArgumentException(
                    String.format("상품 '%s'의 재고가 부족합니다. 요청: %d, 재고: %d",
                        product.name(), item.quantity(), product.stockQuantity()));
            }
        }
    }
    
    /**
     * Saga 상태를 추적하기 위한 내부 클래스
     */
    private static class SagaState {
        private final String sagaId;
        private final Long userId;
        private final List<OrderItemRequest> items;
        private final Long couponId;
        private final BigDecimal totalAmount;
        
        private BigDecimal discountAmount;
        private BigDecimal finalAmount;
        private String paymentId;
        
        public SagaState(OrderSagaEvent.OrderInitiated event) {
            this.sagaId = event.getSagaId();
            this.userId = event.getUserId();
            this.items = event.getItems();
            this.couponId = event.getCouponId();
            this.totalAmount = event.getTotalAmount();
        }
        
        // Getters and Setters
        public String getSagaId() { return sagaId; }
        public Long getUserId() { return userId; }
        public List<OrderItemRequest> getItems() { return items; }
        public Long getCouponId() { return couponId; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
        public BigDecimal getFinalAmount() { return finalAmount; }
        public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    }
}