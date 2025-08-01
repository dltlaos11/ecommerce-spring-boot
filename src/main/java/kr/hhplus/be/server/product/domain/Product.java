package kr.hhplus.be.server.product.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.product.exception.InsufficientStockException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ✅ 현업 스타일: Entity + Domain 통합
 */
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_name", columnList = "name"),
        @Index(name = "idx_products_price", columnList = "price"),
        @Index(name = "idx_products_stock", columnList = "stock_quantity")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ======================== 생성자 ========================

    public Product(String name, BigDecimal price, Integer stockQuantity) {
        validateProductData(name, price, stockQuantity);

        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    // ======================== 비즈니스 로직 (기존 Domain 로직 그대로) ========================

    /**
     * 재고 차감
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
    }

    /**
     * 재고 복구
     */
    public void restoreStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("복구할 수량은 0보다 커야 합니다.");
        }

        this.stockQuantity += quantity;
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
    }

    /**
     * 재고 수량 직접 설정
     */
    public void setStockQuantity(Integer stockQuantity) {
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("재고 수량은 0 이상이어야 합니다.");
        }

        this.stockQuantity = stockQuantity;
    }

    // ======================== JPA를 위한 setter ========================

    void setId(Long id) {
        this.id = id;
    }

    void setName(String name) {
        this.name = name;
    }

    void setPrice(BigDecimal price) {
        this.price = price;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ======================== 비즈니스 로직 헬퍼 ========================

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

    @Override
    public String toString() {
        return String.format("Product{id=%d, name='%s', price=%s, stock=%d}",
                id, name, price, stockQuantity);
    }

    // ========== Product.java 추가 ==========
    /**
     * 테스트 전용 setter 메서드들
     * 
     * @deprecated 테스트에서만 사용
     */
    @Deprecated
    public void setIdForTest(Long id) {
        this.id = id;
    }

    @Deprecated
    public void setCreatedAtForTest(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Deprecated
    public void setUpdatedAtForTest(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}