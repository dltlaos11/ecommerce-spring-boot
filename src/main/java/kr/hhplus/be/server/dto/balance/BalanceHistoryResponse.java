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

// package kr.hhplus.be.server.dto.balance;

// import java.math.BigDecimal;
// import java.time.LocalDateTime;

// import com.fasterxml.jackson.annotation.JsonFormat;

// import io.swagger.v3.oas.annotations.media.Schema;

// @Schema(description = "잔액 변동 이력")
// public class BalanceHistoryResponse {

// @Schema(description = "거래 유형", example = "CHARGE", allowableValues = {
// "CHARGE", "PAYMENT" })
// private String transactionType;

// @Schema(description = "변동 금액", example = "50000.00")
// private BigDecimal amount;

// @Schema(description = "거래 후 잔액", example = "100000.00")
// private BigDecimal balanceAfter;

// @Schema(description = "거래 시간", example = "2025-07-16T10:30:00")
// @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
// private LocalDateTime createdAt;

// // 기본 생성자
// public BalanceHistoryResponse() {
// }

// // 파라미터 생성자
// public BalanceHistoryResponse(String transactionType, BigDecimal amount,
// BigDecimal balanceAfter, LocalDateTime createdAt) {
// this.transactionType = transactionType;
// this.amount = amount;
// this.balanceAfter = balanceAfter;
// this.createdAt = createdAt;
// }

// // 모든 getter/setter 메서드 추가 (Jackson 직렬화를 위해 필수!)
// public String getTransactionType() {
// return transactionType;
// }

// public void setTransactionType(String transactionType) {
// this.transactionType = transactionType;
// }

// public BigDecimal getAmount() {
// return amount;
// }

// public void setAmount(BigDecimal amount) {
// this.amount = amount;
// }

// public BigDecimal getBalanceAfter() {
// return balanceAfter;
// }

// public void setBalanceAfter(BigDecimal balanceAfter) {
// this.balanceAfter = balanceAfter;
// }

// public LocalDateTime getCreatedAt() {
// return createdAt;
// }

// public void setCreatedAt(LocalDateTime createdAt) {
// this.createdAt = createdAt;
// }
// }
