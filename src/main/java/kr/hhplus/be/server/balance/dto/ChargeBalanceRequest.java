package kr.hhplus.be.server.balance.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Schema(description = "잔액 충전 요청")
public record ChargeBalanceRequest(
        @NotNull(message = "충전 금액은 필수입니다.") @DecimalMin(value = "1000.0", message = "최소 충전 금액은 1,000원입니다.") @DecimalMax(value = "1000000.0", message = "최대 충전 금액은 1,000,000원입니다.") @Schema(description = "충전할 금액", example = "50000.00", minimum = "1000.0", maximum = "1000000.0") BigDecimal amount) {
}