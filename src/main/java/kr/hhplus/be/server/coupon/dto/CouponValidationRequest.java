package kr.hhplus.be.server.coupon.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "쿠폰 사용 가능 여부 확인 요청")
public record CouponValidationRequest(
        @NotNull(message = "사용자 ID는 필수입니다.") @Schema(description = "사용자 ID", example = "1") Long userId,

        @NotNull(message = "쿠폰 ID는 필수입니다.") @Schema(description = "쿠폰 ID", example = "1") Long couponId,

        @NotNull(message = "주문 금액은 필수입니다.") @Positive(message = "주문 금액은 0보다 커야 합니다.") @Schema(description = "주문 금액", example = "150000.00") BigDecimal orderAmount) {
}