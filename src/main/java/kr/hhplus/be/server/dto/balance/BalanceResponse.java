package kr.hhplus.be.server.dto.balance;

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

// package kr.hhplus.be.server.dto.balance;

// import java.math.BigDecimal;
// import java.time.LocalDateTime;

// import com.fasterxml.jackson.annotation.JsonFormat;

// import io.swagger.v3.oas.annotations.media.Schema;

// @Schema(description = "잔액 조회 응답")
// public class BalanceResponse {

// @Schema(description = "사용자 ID", example = "1")
// private Long userId;

// @Schema(description = "현재 잔액", example = "50000.00")
// private BigDecimal balance;

// @Schema(description = "마지막 업데이트 시간", example = "2025-07-16T10:30:00")
// @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
// private LocalDateTime lastUpdated;

// // 기본 생성자
// public BalanceResponse() {
// }

// public BalanceResponse(Long userId, BigDecimal balance, LocalDateTime
// lastUpdated) {
// this.userId = userId;
// this.balance = balance;
// this.lastUpdated = lastUpdated;
// }

// // 모든 getter 메서드 추가 (Jackson 직렬화를 위해 필수!)
// public Long getUserId() {
// return userId;
// }

// public void setUserId(Long userId) {
// this.userId = userId;
// }

// public BigDecimal getBalance() {
// return balance;
// }

// public void setBalance(BigDecimal balance) {
// this.balance = balance;
// }

// public LocalDateTime getLastUpdated() {
// return lastUpdated;
// }

// public void setLastUpdated(LocalDateTime lastUpdated) {
// this.lastUpdated = lastUpdated;
// }
// }
