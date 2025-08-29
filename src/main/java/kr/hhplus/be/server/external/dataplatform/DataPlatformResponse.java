package kr.hhplus.be.server.external.dataplatform;

import java.time.LocalDateTime;

/**
 * 데이터 플랫폼 응답 DTO
 */
public record DataPlatformResponse(
    boolean success,
    String messageId,
    String message,
    LocalDateTime processedAt
) {
    
    public static DataPlatformResponse success(String messageId) {
        return new DataPlatformResponse(
            true, 
            messageId, 
            "데이터 플랫폼 전송 성공", 
            LocalDateTime.now()
        );
    }
    
    public static DataPlatformResponse failure(String message) {
        return new DataPlatformResponse(
            false, 
            null, 
            message, 
            LocalDateTime.now()
        );
    }
}