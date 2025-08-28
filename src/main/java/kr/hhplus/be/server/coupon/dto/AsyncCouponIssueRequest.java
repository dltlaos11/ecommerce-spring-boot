package kr.hhplus.be.server.coupon.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 비동기 쿠폰 발급 요청 DTO
 */
public record AsyncCouponIssueRequest(
    @NotNull(message = "사용자 ID는 필수입니다.")
    Long userId,
    
    @NotNull(message = "쿠폰 ID는 필수입니다.")
    Long couponId
) {
}