package kr.hhplus.be.server.order.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 주문 도메인 모델
 * 
 * 설계 원칙:
 * - 주문 상태 관리 및 금액 계산 로직 캡슐화
 * - 주문 항목들과의 관계 관리
 * - 결제 및 배송 상태 추적
 * 
 * 책임:
 * - 주문 상태 변경 로직
 * - 총 금액 계산 및 검증
 * - 주문 완료 조건 확인
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private Long id;
    private String orderNumber;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private Long couponId;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 주문 항목들 (연관관계)
    private List<OrderItem> orderItems = new ArrayList<>();

    /**
     * 주문 상태 enum
     */
    public enum OrderStatus {
        PENDING("PENDING", "주문 대기"),
        COMPLETED("COMPLETED", "주문 완료"),
        CANCELLED("CANCELLED", "주문 취소"),
        FAILED("FAILED", "주문 실패");

        private final String code;
        private final String description;

        OrderStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 새 주문 생성용 생성자
     */
    public Order(String orderNumber, Long userId, BigDecimal totalAmount,
            BigDecimal discountAmount, BigDecimal finalAmount, Long couponId) {
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.finalAmount = finalAmount;
        this.couponId = couponId;
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.orderItems = new ArrayList<>();
    }

    /**
     * 주문 완료 처리
     */
    public void complete() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("대기 중인 주문만 완료할 수 있습니다.");
        }
        this.status = OrderStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 주문 취소 처리
     */
    public void cancel() {
        if (this.status == OrderStatus.COMPLETED) {
            throw new IllegalStateException("완료된 주문은 취소할 수 없습니다.");
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 주문 실패 처리
     */
    public void fail() {
        this.status = OrderStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 주문 항목 추가
     */
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    /**
     * 총 금액 재계산 (주문 항목 기준)
     */
    public void recalculateTotalAmount() {
        this.totalAmount = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 최종 금액도 재계산
        this.finalAmount = this.totalAmount.subtract(this.discountAmount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 주문 완료 가능 여부 확인
     */
    public boolean canComplete() {
        return this.status == OrderStatus.PENDING &&
                this.orderItems != null &&
                !this.orderItems.isEmpty();
    }

    /**
     * 주문 취소 가능 여부 확인
     */
    public boolean canCancel() {
        return this.status == OrderStatus.PENDING || this.status == OrderStatus.FAILED;
    }

    /**
     * 쿠폰 사용 여부
     */
    public boolean hasCoupon() {
        return this.couponId != null;
    }

    /**
     * ID 설정 (Repository에서 호출)
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * updatedAt 갱신 (Repository 저장 시)
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 상태 설정 (Repository에서 호출)
     */
    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    /**
     * 디버깅 및 로깅용 toString
     */
    @Override
    public String toString() {
        return String.format("Order{id=%d, orderNumber='%s', userId=%d, status=%s, finalAmount=%s}",
                id, orderNumber, userId, status, finalAmount);
    }
}
