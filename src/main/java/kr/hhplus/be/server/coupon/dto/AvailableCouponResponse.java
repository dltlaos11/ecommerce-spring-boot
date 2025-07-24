package kr.hhplus.be.server.coupon.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "발급 가능한 쿠폰 정보")
public record AvailableCouponResponse(
        @Schema(description = "쿠폰 ID", example = "1") Long id,

        @Schema(description = "쿠폰명", example = "10% 할인 쿠폰") String name,

        @Schema(description = "할인 타입", example = "PERCENTAGE", allowableValues = {
                "FIXED", "PERCENTAGE" }) String discountType,

        @Schema(description = "할인 값", example = "10.00") BigDecimal discountValue,

        @Schema(description = "최대 할인 금액", example = "50000.00") BigDecimal maxDiscountAmount,

        @Schema(description = "최소 주문 금액", example = "100000.00") BigDecimal minimumOrderAmount,

        @Schema(description = "남은 수량", example = "95") Integer remainingQuantity,

        @Schema(description = "총 수량", example = "100") Integer totalQuantity,

        @Schema(description = "만료일", example = "2025-07-31T23:59:59") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime expiredAt){
}