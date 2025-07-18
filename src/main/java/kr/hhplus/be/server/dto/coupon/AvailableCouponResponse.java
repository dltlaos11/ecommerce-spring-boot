package kr.hhplus.be.server.dto.coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

// 사용 가능한 쿠폰 응답
@Schema(description = "사용 가능한 쿠폰 정보")
public class AvailableCouponResponse {

    @Schema(description = "쿠폰 ID", example = "1")
    private Long id;

    @Schema(description = "쿠폰명", example = "10% 할인 쿠폰")
    private String name;

    @Schema(description = "할인 타입", example = "PERCENTAGE", allowableValues = { "FIXED", "PERCENTAGE" })
    private String discountType;

    @Schema(description = "할인 값", example = "10.00")
    private BigDecimal discountValue;

    @Schema(description = "최대 할인 금액", example = "50000.00")
    private BigDecimal maxDiscountAmount;

    @Schema(description = "최소 주문 금액", example = "100000.00")
    private BigDecimal minimumOrderAmount;

    @Schema(description = "남은 수량", example = "95")
    private Integer remainingQuantity;

    @Schema(description = "총 수량", example = "100")
    private Integer totalQuantity;

    @Schema(description = "만료일", example = "2025-07-23T23:59:59")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiredAt;

    // 기본 생성자
    public AvailableCouponResponse() {
    }

    public AvailableCouponResponse(Long id, String name, String discountType, BigDecimal discountValue,
            BigDecimal maxDiscountAmount, BigDecimal minimumOrderAmount,
            Integer remainingQuantity, Integer totalQuantity, LocalDateTime expiredAt) {
        this.id = id;
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minimumOrderAmount = minimumOrderAmount;
        this.remainingQuantity = remainingQuantity;
        this.totalQuantity = totalQuantity;
        this.expiredAt = expiredAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public BigDecimal getMinimumOrderAmount() {
        return minimumOrderAmount;
    }

    public void setMinimumOrderAmount(BigDecimal minimumOrderAmount) {
        this.minimumOrderAmount = minimumOrderAmount;
    }

    public Integer getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(Integer remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }
}