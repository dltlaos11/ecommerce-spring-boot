package kr.hhplus.be.server.order.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entity + Domain
 * 연관관계 매핑 제거
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_order_number", columnList = "order_number", unique = true),
        @Index(name = "idx_orders_user_created", columnList = "user_id, created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId; // 연관관계 없이 단순 FK

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "final_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal finalAmount;

    @Column(name = "coupon_id")
    private Long couponId; // 연관관계 없이 단순 FK

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

    // 생성자

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
    }

    // 비즈니스 로직 (기존 Domain 로직 그대로)

    /**
     * 주문 완료 처리
     */
    public void complete() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("대기 중인 주문만 완료할 수 있습니다.");
        }
        this.status = OrderStatus.COMPLETED;
    }

    /**
     * 주문 취소 처리
     */
    public void cancel() {
        if (this.status == OrderStatus.COMPLETED) {
            throw new IllegalStateException("완료된 주문은 취소할 수 없습니다.");
        }
        this.status = OrderStatus.CANCELLED;
    }

    /**
     * 주문 실패 처리
     */
    public void fail() {
        this.status = OrderStatus.FAILED;
    }

    /**
     * 주문 완료 가능 여부 확인
     */
    public boolean canComplete() {
        return this.status == OrderStatus.PENDING;
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

    // JPA를 위한 setter

    void setId(Long id) {
        this.id = id;
    }

    void setStatus(OrderStatus status) {
        this.status = status;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return String.format("Order{id=%d, orderNumber='%s', userId=%d, status=%s, finalAmount=%s}",
                id, orderNumber, userId, status, finalAmount);
    }
}