
package kr.hhplus.be.server.dto.coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

// 발급된 쿠폰 응답
@Schema(description = "발급된 쿠폰 정보")
public class IssuedCouponResponse {

    @Schema(description = "사용자 쿠폰 ID", example = "123")
    private Long userCouponId;

    @Schema(description = "쿠폰 ID", example = "1")
    private Long couponId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "쿠폰명", example = "10% 할인 쿠폰")
    private String couponName;

    @Schema(description = "할인 타입", example = "PERCENTAGE", allowableValues = { "FIXED", "PERCENTAGE" })
    private String discountType;

    @Schema(description = "할인 값", example = "10.00")
    private BigDecimal discountValue;

    @Schema(description = "쿠폰 만료일", example = "2025-07-23T23:59:59")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiredAt;

    @Schema(description = "발급 시간", example = "2025-07-16T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime issuedAt;

    @Schema(description = "쿠폰 상태", example = "AVAILABLE", allowableValues = { "AVAILABLE", "USED", "EXPIRED" })
    private String status;

    // 기본 생성자
    public IssuedCouponResponse() {
    }

    public IssuedCouponResponse(Long userCouponId, Long couponId, Long userId, String couponName,
            String discountType, BigDecimal discountValue, LocalDateTime expiredAt,
            LocalDateTime issuedAt, String status) {
        this.userCouponId = userCouponId;
        this.couponId = couponId;
        this.userId = userId;
        this.couponName = couponName;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.expiredAt = expiredAt;
        this.issuedAt = issuedAt;
        this.status = status;
    }

    // Getters and Setters
    public Long getUserCouponId() {
        return userCouponId;
    }

    public void setUserCouponId(Long userCouponId) {
        this.userCouponId = userCouponId;
    }

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCouponName() {
        return couponName;
    }

    public void setCouponName(String couponName) {
        this.couponName = couponName;
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

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}