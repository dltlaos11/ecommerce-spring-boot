package kr.hhplus.be.server.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 비동기 쿠폰 발급 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AsyncCouponIssueRequest {
    
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;
    
    @NotNull(message = "쿠폰 ID는 필수입니다.")
    private Long couponId;
}