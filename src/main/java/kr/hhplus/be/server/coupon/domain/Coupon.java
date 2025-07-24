package kr.hhplus.be.server.coupon.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.coupon.exception.CouponExhaustedException;
import kr.hhplus.be.server.coupon.exception.CouponExpiredException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 쿠폰 도메인 모델
 * 
 * 설계 원칙:
 * - 쿠폰 발급 및 사용 비즈니스 로직 캡슐화
 * - 할인 계산 로직 내장
 * - 쿠폰 상태 관리 (유효성 검증)
 * 
 * 책임:
 * - 쿠폰 발급 가능 여부 확인
 * - 할인 금액 계산
 * - 쿠폰 유효성 검증 (만료일, 최소 주문금액)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

    private Long id;
    private String name;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Integer totalQuantity;
    private Integer issuedQuantity;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minimumOrderAmount;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
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
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰 발급 가능 여부 확인
     * 
     * 🎯 비즈니스 규칙:
     * - 쿠폰이 만료되지 않았는지 확인
     * - 발급 수량이 남아있는지 확인
     * 
     * @throws CouponExpiredException   쿠폰이 만료된 경우
     * @throws CouponExhaustedException 쿠폰이 모두 소진된 경우
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
     * 
     * @param orderAmount 주문 금액
     * @throws CouponExpiredException   쿠폰이 만료된 경우
     * @throws IllegalArgumentException 최소 주문 금액 미달인 경우
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
     * 
     * @param orderAmount 주문 금액
     * @return 실제 할인 금액
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
        this.updatedAt = LocalDateTime.now();
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
     * 발급 수량 설정 (Repository에서 호출)
     */
    public void setIssuedQuantity(Integer issuedQuantity) {
        this.issuedQuantity = issuedQuantity;
    }

    /**
     * 디버깅 및 로깅용 toString
     */
    @Override
    public String toString() {
        return String.format("Coupon{id=%d, name='%s', type=%s, issued=%d/%d}",
                id, name, discountType, issuedQuantity, totalQuantity);
    }
}