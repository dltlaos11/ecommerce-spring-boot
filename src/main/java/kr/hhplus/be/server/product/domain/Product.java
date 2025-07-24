package kr.hhplus.be.server.product.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.product.exception.InsufficientStockException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 상품 도메인 모델
 * 
 * ✨ 설계 원칙:
 * - 비즈니스 로직을 도메인 객체 내부에 캡슐화
 * - 데이터와 행위를 함께 관리하여 응집도 향상
 * - 불변성을 보장하는 메서드 제공
 * 
 * 🎯 책임:
 * - 재고 관리 (차감, 복구, 확인)
 * - 데이터 유효성 검증
 * - 상품 상태 관리
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 새 상품 생성용 생성자 (ID는 Repository에서 자동 할당)
     */
    public Product(String name, BigDecimal price, Integer stockQuantity) {
        validateProductData(name, price, stockQuantity);

        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 재고 차감
     * 
     * 🎯 비즈니스 규칙:
     * - 재고가 충분한지 먼저 검증
     * - 재고 부족 시 명확한 예외 발생
     * - 차감 후 updatedAt 자동 갱신
     */
    public void reduceStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("차감할 수량은 0보다 커야 합니다.");
        }

        if (!hasEnoughStock(quantity)) {
            throw new InsufficientStockException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    String.format("재고 부족: 요청 수량 %d, 현재 재고 %d", quantity, this.stockQuantity));
        }

        this.stockQuantity -= quantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 재고 복구 (주문 취소, 결제 실패 시)
     */
    public void restoreStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("복구할 수량은 0보다 커야 합니다.");
        }

        this.stockQuantity += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 재고 충분 여부 확인
     */
    public boolean hasEnoughStock(int quantity) {
        return this.stockQuantity >= quantity;
    }

    /**
     * 판매 가능한 상품인지 확인
     */
    public boolean isAvailable() {
        return this.stockQuantity > 0;
    }

    /**
     * 상품 정보 업데이트
     */
    public void updateProductInfo(String name, BigDecimal price) {
        validateProductData(name, price, this.stockQuantity);

        this.name = name;
        this.price = price;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 재고 수량 직접 설정 (관리자 기능)
     */
    public void setStockQuantity(Integer stockQuantity) {
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("재고 수량은 0 이상이어야 합니다.");
        }

        this.stockQuantity = stockQuantity;
        this.updatedAt = LocalDateTime.now();
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
     * 상품 데이터 유효성 검증
     */
    private void validateProductData(String name, BigDecimal price, Integer stockQuantity) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("상품 가격은 0 이상이어야 합니다.");
        }

        if (stockQuantity == null || stockQuantity < 0) {
            throw new IllegalArgumentException("재고 수량은 0 이상이어야 합니다.");
        }
    }

    /**
     * 디버깅 및 로깅용 toString
     */
    @Override
    public String toString() {
        return String.format("Product{id=%d, name='%s', price=%s, stock=%d}",
                id, name, price, stockQuantity);
    }
}