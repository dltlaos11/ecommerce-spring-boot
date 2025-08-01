package kr.hhplus.be.server.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "쿠폰 발급 요청")
public record IssueCouponRequest(
        @NotNull(message = "사용자 ID는 필수입니다.") @Schema(description = "사용자 ID", example = "1") Long userId) {
}