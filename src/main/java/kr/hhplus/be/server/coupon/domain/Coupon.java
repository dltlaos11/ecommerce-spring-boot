package kr.hhplus.be.server.coupon.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.coupon.exception.CouponExhaustedException;
import kr.hhplus.be.server.coupon.exception.CouponExpiredException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entity + Domain 통합
 */
@Entity
@Table(name = "coupons", indexes = {
        @Index(name = "idx_coupons_availability", columnList = "expired_at, issued_quantity, total_quantity")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal discountValue;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "issued_quantity", nullable = false)
    private Integer issuedQuantity = 0;

    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "minimum_order_amount", precision = 10, scale = 2)
    private BigDecimal minimumOrderAmount = BigDecimal.ZERO;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 할인 타입 enum
     */
    public enum DiscountType {
        FIXED("FIXED", "정액 할인"),
        PERCENTAGE("PERCENTAGE", "정률 할인");

        private final String code;
        private final String description;

        DiscountType(String code, String description) {
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
     * 새 쿠폰 생성용 생성자
     */
    public Coupon(String name, DiscountType discountType, BigDecimal discountValue,
            Integer totalQuantity, BigDecimal maxDiscountAmount,
            BigDecimal minimumOrderAmount, LocalDateTime expiredAt) {
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minimumOrderAmount = minimumOrderAmount != null ? minimumOrderAmount : BigDecimal.ZERO;
        this.expiredAt = expiredAt;
    }

    // 비즈니스 로직 (기존 Domain 로직 그대로)

    /**
     * 쿠폰 발급 가능 여부 확인
     */
    public void validateIssuable() {
        if (isExpired()) {
            throw new CouponExpiredException(ErrorCode.COUPON_EXPIRED);
        }

        if (isExhausted()) {
            throw new CouponExhaustedException(ErrorCode.COUPON_EXHAUSTED);
        }
    }

    /**
     * 쿠폰 사용 가능 여부 확인
     */
    public void validateUsable(BigDecimal orderAmount) {
        if (isExpired()) {
            throw new CouponExpiredException(ErrorCode.COUPON_EXPIRED);
        }

        if (orderAmount.compareTo(minimumOrderAmount) < 0) {
            throw new IllegalArgumentException(
                    String.format("최소 주문 금액 %s원 이상이어야 합니다.", minimumOrderAmount));
        }
    }

    /**
     * 할인 금액 계산
     */
    public BigDecimal calculateDiscountAmount(BigDecimal orderAmount) {
        validateUsable(orderAmount);

        BigDecimal discountAmount;

        if (discountType == DiscountType.FIXED) {
            // 정액 할인
            discountAmount = discountValue;
        } else {
            // 정률 할인 (퍼센트)
            discountAmount = orderAmount.multiply(discountValue).divide(new BigDecimal("100"));
        }

        // 최대 할인 금액 제한
        if (maxDiscountAmount != null && discountAmount.compareTo(maxDiscountAmount) > 0) {
            discountAmount = maxDiscountAmount;
        }

        // 주문 금액보다 클 수는 없음
        if (discountAmount.compareTo(orderAmount) > 0) {
            discountAmount = orderAmount;
        }

        return discountAmount;
    }

    /**
     * 쿠폰 발급 처리 (발급 수량 증가)
     */
    public void issue() {
        validateIssuable();
        this.issuedQuantity++;
    }

    /**
     * 만료 여부 확인
     */
    public boolean isExpired() {
        return expiredAt != null && LocalDateTime.now().isAfter(expiredAt);
    }

    /**
     * 소진 여부 확인
     */
    public boolean isExhausted() {
        return issuedQuantity >= totalQuantity;
    }

    /**
     * 남은 수량
     */
    public Integer getRemainingQuantity() {
        return totalQuantity - issuedQuantity;
    }

    /**
     * 발급 가능 여부
     */
    public boolean isAvailable() {
        return !isExpired() && !isExhausted();
    }

    // JPA를 위한 setter

    void setId(Long id) {
        this.id = id;
    }

    void setIssuedQuantity(Integer issuedQuantity) {
        this.issuedQuantity = issuedQuantity;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return String.format("Coupon{id=%d, name='%s', type=%s, issued=%d/%d}",
                id, name, discountType, issuedQuantity, totalQuantity);
    }

    // ========== Coupon.java 추가 ==========
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
    public void setIssuedQuantityForTest(Integer issuedQuantity) {
        this.issuedQuantity = issuedQuantity;
    }

    @Deprecated
    public void setCreatedAtForTest(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Deprecated
    public void setUpdatedAtForTest(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Deprecated
    public void setMaxDiscountAmountForTest(BigDecimal maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    @Deprecated
    public void setMinimumOrderAmountForTest(BigDecimal minimumOrderAmount) {
        this.minimumOrderAmount = minimumOrderAmount;
    }

}