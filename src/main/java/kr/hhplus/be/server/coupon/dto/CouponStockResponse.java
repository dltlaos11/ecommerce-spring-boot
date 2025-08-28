package kr.hhplus.be.server.coupon.dto;

import java.time.LocalDateTime;

/**
 * 쿠폰 재고 조회 응답 DTO
 */
public record CouponStockResponse(
    Long couponId,
    Object currentStock, // Integer 또는 String("초기화 필요")
    LocalDateTime timestamp
) {
    
    public static CouponStockResponse of(Long couponId, Integer currentStock) {
        return new CouponStockResponse(
            couponId,
            currentStock != null ? currentStock : "초기화 필요",
            LocalDateTime.now()
        );
    }
}