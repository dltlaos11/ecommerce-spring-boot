package kr.hhplus.be.server.balance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "잔액 조회 응답")
public record BalanceResponse(
        @Schema(description = "사용자 ID", example = "1") Long userId,

        @Schema(description = "현재 잔액", example = "50000.00") BigDecimal balance,

        @Schema(description = "마지막 업데이트 시간", example = "2025-07-17T10:30:00") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime lastUpdated) {
}