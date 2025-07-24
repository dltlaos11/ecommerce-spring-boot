package kr.hhplus.be.server.balance.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "잔액 충전 응답")
public record ChargeBalanceResponse(
        @Schema(description = "사용자 ID", example = "1") Long userId,

        @Schema(description = "충전 전 잔액", example = "50000.00") BigDecimal previousBalance,

        @Schema(description = "충전 금액", example = "30000.00") BigDecimal chargedAmount,

        @Schema(description = "충전 후 잔액", example = "80000.00") BigDecimal currentBalance,

        @Schema(description = "거래 ID", example = "TXN_1721124600000_A1B2C3D4") String transactionId) {
}