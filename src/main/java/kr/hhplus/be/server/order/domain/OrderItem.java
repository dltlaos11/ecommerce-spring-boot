package kr.hhplus.be.server.order.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 주문 항목 도메인 모델
 * 
 * 설계 원칙:
 * - 주문 시점의 상품 정보 보존 (비정규화)
 * - 소계 금액 계산 로직 내장
 * - 주문과의 연관관계 관리
 * 
 * 책임:
 * - 주문 항목별 소계 계산
 * - 주문 시점 상품 정보 보존
 * - 수량 및 가격 검증
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    private Long id;
    private Order order; // 연관관계
    private Long productId;
    private String productName; // 주문 시점 상품명 보존
    private BigDecimal productPrice; // 주문 시점 상품가격 보존
    private Integer quantity;
    private BigDecimal subtotal;
    private LocalDateTime createdAt;

    /**
     * 새 주문 항목 생성용 생성자
     */
    public OrderItem(Long productId, String productName, BigDecimal productPrice, Integer quantity) {
        validateInputs(productId, productName, productPrice, quantity);

        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.subtotal = calculateSubtotal(productPrice, quantity);
        this.createdAt = LocalDateTime.now();
    }

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

    /**
     * ID 설정 (Repository에서 호출)
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Order 설정 (연관관계 편의 메서드)
     */
    public void setOrder(Order order) {
        this.order = order;
    }

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

    /**
     * 디버깅 및 로깅용 toString
     */
    @Override
    public String toString() {
        return String.format("OrderItem{productId=%d, productName='%s', quantity=%d, subtotal=%s}",
                productId, productName, quantity, subtotal);
    }
}