package kr.hhplus.be.server.coupon.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "쿠폰 사용 가능 여부 확인 응답")
public record CouponValidationResponse(
        @Schema(description = "쿠폰 ID", example = "1") Long couponId,

        @Schema(description = "사용자 ID", example = "1") Long userId,

        @Schema(description = "사용 가능 여부", example = "true") Boolean usable,

        @Schema(description = "할인 금액", example = "15000.00") BigDecimal discountAmount,

        @Schema(description = "최종 결제 금액", example = "135000.00") BigDecimal finalAmount,

        @Schema(description = "사용 불가 사유 (사용 가능한 경우 null)", example = "쿠폰이 만료되었습니다.") String reason) {
}