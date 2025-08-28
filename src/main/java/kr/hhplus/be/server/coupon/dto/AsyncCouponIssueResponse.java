package kr.hhplus.be.server.coupon.dto;

import java.time.LocalDateTime;

/**
 * 비동기 쿠폰 발급 응답 DTO
 */
public record AsyncCouponIssueResponse(
    String requestId,           // 요청 추적 ID
    String status,              // 요청 상태 (PENDING, PROCESSING, COMPLETED, FAILED)
    String message,             // 상태 메시지
    LocalDateTime requestedAt,  // 요청 시간
    LocalDateTime completedAt,  // 완료 시간 (완료된 경우)
    Long issuedCouponId        // 발급된 쿠폰 ID (성공한 경우)
) {
    
    public static AsyncCouponIssueResponse pending(String requestId, LocalDateTime requestedAt) {
        return new AsyncCouponIssueResponse(
            requestId, 
            "PENDING", 
            "쿠폰 발급 요청이 대기열에 추가되었습니다.", 
            requestedAt, 
            null, 
            null
        );
    }
    
    public static AsyncCouponIssueResponse processing(String requestId, LocalDateTime requestedAt) {
        return new AsyncCouponIssueResponse(
            requestId, 
            "PROCESSING", 
            "쿠폰 발급을 처리 중입니다.", 
            requestedAt, 
            null, 
            null
        );
    }
    
    public static AsyncCouponIssueResponse completed(String requestId, LocalDateTime requestedAt, 
                                                   LocalDateTime completedAt, Long issuedCouponId) {
        return new AsyncCouponIssueResponse(
            requestId, 
            "COMPLETED", 
            "쿠폰이 성공적으로 발급되었습니다.", 
            requestedAt, 
            completedAt, 
            issuedCouponId
        );
    }
    
    public static AsyncCouponIssueResponse failed(String requestId, LocalDateTime requestedAt, 
                                                 LocalDateTime completedAt, String errorMessage) {
        return new AsyncCouponIssueResponse(
            requestId, 
            "FAILED", 
            errorMessage, 
            requestedAt, 
            completedAt, 
            null
        );
    }
}