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
 * Entity + Domain 통합 - 불필요한 인덱스 제거
 * 실제 쿼리 패턴에 맞는 최소한의 인덱스만 유지
 */
@Entity
@Table(name = "products") // 불필요한 인덱스 제거
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

    // 생성자
    public Product(String name, BigDecimal price, Integer stockQuantity) {
        validateProductData(name, price, stockQuantity);

        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    // 비즈니스 로직 (동일)
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

    public void restoreStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("복구할 수량은 0보다 커야 합니다.");
        }

        this.stockQuantity += quantity;
    }

    public boolean hasEnoughStock(int quantity) {
        return this.stockQuantity >= quantity;
    }

    public boolean isAvailable() {
        return this.stockQuantity > 0;
    }

    public void updateProductInfo(String name, BigDecimal price) {
        validateProductData(name, price, this.stockQuantity);

        this.name = name;
        this.price = price;
    }

    public void setStockQuantity(Integer stockQuantity) {
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("재고 수량은 0 이상이어야 합니다.");
        }

        this.stockQuantity = stockQuantity;
    }

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

    // 테스트 전용 메서드들
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