package kr.hhplus.be.server.dto.balance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "잔액 변동 이력")
public record BalanceHistoryResponse(
                @Schema(description = "거래 유형", example = "CHARGE", allowableValues = {
                                "CHARGE", "PAYMENT" }) String transactionType,

                @Schema(description = "변동 금액", example = "50000.00") BigDecimal amount,

                @Schema(description = "거래 후 잔액", example = "100000.00") BigDecimal balanceAfter,

                @Schema(description = "거래 시간", example = "2025-07-17T10:30:00") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt){
}