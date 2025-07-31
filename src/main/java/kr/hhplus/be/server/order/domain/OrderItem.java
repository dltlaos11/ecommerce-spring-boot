package kr.hhplus.be.server.order.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
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
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_items_order_id", columnList = "order_id"),
        @Index(name = "idx_order_items_product", columnList = "product_id"),
        @Index(name = "idx_order_items_created_product", columnList = "created_at, product_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId; // 연관관계 없이 단순 FK

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName; // 주문 시점 상품명 보존 (비정규화)

    @Column(name = "product_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal productPrice; // 주문 시점 상품가격 보존 (비정규화)

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "subtotal", precision = 15, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 생성자

    /**
     * 새 주문 항목 생성용 생성자
     */
    public OrderItem(Long orderId, Long productId, String productName,
            BigDecimal productPrice, Integer quantity) {
        validateInputs(productId, productName, productPrice, quantity);

        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.subtotal = calculateSubtotal(productPrice, quantity);
    }

    // 비즈니스 로직 (기존 Domain 로직 그대로)

    /**
     * 소계 계산
     */
    public static BigDecimal calculateSubtotal(BigDecimal price, Integer quantity) {
        if (price == null || quantity == null || quantity <= 0) {
            return BigDecimal.ZERO;
        }
        return price.multiply(new BigDecimal(quantity));
    }

    /**
     * 소계 재계산
     */
    public void recalculateSubtotal() {
        this.subtotal = calculateSubtotal(this.productPrice, this.quantity);
    }

    /**
     * 수량 변경
     */
    public void changeQuantity(Integer newQuantity) {
        if (newQuantity == null || newQuantity <= 0) {
            throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
        }
        this.quantity = newQuantity;
        recalculateSubtotal();
    }

    // JPA를 위한 setter

    void setId(Long id) {
        this.id = id;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // 비즈니스 로직 헬퍼

    /**
     * 입력값 검증
     */
    private void validateInputs(Long productId, String productName, BigDecimal productPrice, Integer quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 필수입니다.");
        }
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        if (productPrice == null || productPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("상품 가격은 0 이상이어야 합니다.");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
        }
    }

    @Override
    public String toString() {
        return String.format("OrderItem{id=%d, orderId=%d, productId=%d, productName='%s', quantity=%d, subtotal=%s}",
                id, orderId, productId, productName, quantity, subtotal);
    }
}