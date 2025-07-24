package kr.hhplus.be.server.coupon.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 보유 쿠폰 정보")
public record UserCouponResponse(
        @Schema(description = "사용자 쿠폰 ID", example = "123") Long userCouponId,

        @Schema(description = "쿠폰 ID", example = "1") Long couponId,

        @Schema(description = "쿠폰명", example = "10% 할인 쿠폰") String couponName,

        @Schema(description = "할인 타입", example = "PERCENTAGE") String discountType,

        @Schema(description = "할인 값", example = "10.00") BigDecimal discountValue,

        @Schema(description = "최대 할인 금액", example = "50000.00") BigDecimal maxDiscountAmount,

        @Schema(description = "최소 주문 금액", example = "100000.00") BigDecimal minimumOrderAmount,

        @Schema(description = "쿠폰 상태", example = "AVAILABLE") String status,

        @Schema(description = "쿠폰 만료일", example = "2025-07-31T23:59:59") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime expiredAt,

        @Schema(description = "발급 시간", example = "2025-07-25T10:30:00") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime issuedAt,

        @Schema(description = "사용 시간", example = "2025-07-26T15:20:00") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime usedAt) {
}