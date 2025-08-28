package kr.hhplus.be.server.coupon.dto;

import java.time.LocalDateTime;

/**
 * 시스템 상태 조회 응답 DTO
 */
public record SystemStatusResponse(
    Long queueSize,
    String systemHealth,
    String error,
    LocalDateTime timestamp
) {
    
    public static SystemStatusResponse ok(Long queueSize) {
        return new SystemStatusResponse(
            queueSize != null ? queueSize : 0,
            "OK",
            null,
            LocalDateTime.now()
        );
    }
    
    public static SystemStatusResponse error(String errorMessage) {
        return new SystemStatusResponse(
            -1L,
            "ERROR", 
            errorMessage,
            LocalDateTime.now()
        );
    }
}