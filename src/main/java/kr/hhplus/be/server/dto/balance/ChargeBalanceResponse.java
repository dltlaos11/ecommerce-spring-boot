package kr.hhplus.be.server.dto.balance;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "잔액 충전 응답")
public record ChargeBalanceResponse(
        @Schema(description = "사용자 ID", example = "1") Long userId,

        @Schema(description = "충전 전 잔액", example = "50000.00") BigDecimal previousBalance,

        @Schema(description = "충전 금액", example = "30000.00") BigDecimal chargedAmount,

        @Schema(description = "충전 후 잔액", example = "80000.00") BigDecimal currentBalance,

        @Schema(description = "거래 ID", example = "tx_1721124600000") String transactionId) {
}

// package kr.hhplus.be.server.dto.balance;

// import java.math.BigDecimal;

// import io.swagger.v3.oas.annotations.media.Schema;

// @Schema(description = "잔액 충전 응답")
// public class ChargeBalanceResponse {

// @Schema(description = "사용자 ID", example = "1")
// private Long userId;

// @Schema(description = "충전 전 잔액", example = "50000.00")
// private BigDecimal previousBalance;

// @Schema(description = "충전 금액", example = "30000.00")
// private BigDecimal chargedAmount;

// @Schema(description = "충전 후 잔액", example = "80000.00")
// private BigDecimal currentBalance;

// @Schema(description = "거래 ID", example = "tx_1721124600000")
// private String transactionId;

// // 기본 생성자
// public ChargeBalanceResponse() {
// }

// public ChargeBalanceResponse(Long userId, BigDecimal previousBalance,
// BigDecimal chargedAmount,
// BigDecimal currentBalance, String transactionId) {
// this.userId = userId;
// this.previousBalance = previousBalance;
// this.chargedAmount = chargedAmount;
// this.currentBalance = currentBalance;
// this.transactionId = transactionId;
// }

// // 모든 getter/setter 메서드 (Jackson 직렬화를 위해 필수!)
// public Long getUserId() {
// return userId;
// }

// public void setUserId(Long userId) {
// this.userId = userId;
// }

// public BigDecimal getPreviousBalance() {
// return previousBalance;
// }

// public void setPreviousBalance(BigDecimal previousBalance) {
// this.previousBalance = previousBalance;
// }

// public BigDecimal getChargedAmount() {
// return chargedAmount;
// }

// public void setChargedAmount(BigDecimal chargedAmount) {
// this.chargedAmount = chargedAmount;
// }

// public BigDecimal getCurrentBalance() {
// return currentBalance;
// }

// public void setCurrentBalance(BigDecimal currentBalance) {
// this.currentBalance = currentBalance;
// }

// public String getTransactionId() {
// return transactionId;
// }

// public void setTransactionId(String transactionId) {
// this.transactionId = transactionId;
// }
// }
