package kr.hhplus.be.server.balance.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 잔액 변동 이력 도메인 모델
 * 
 * 설계 원칙:
 * - 불변 데이터 (생성 후 수정 불가)
 * - 감사(Audit) 목적의 이력 데이터
 * - 잔액 추적 및 문제 해결 지원
 * 
 * 책임:
 * - 잔액 변동 내역 기록
 * - 거래 추적 정보 제공
 * - 잔액 계산 검증 지원
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceHistory {

    private Long id;
    private Long userId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter; // 거래 후 잔액 (비정규화로 조회 성능 향상)
    private String description;
    private String transactionId; // 외부 거래 ID (결제 시스템 연동용)
    private LocalDateTime createdAt;

    /**
     * 거래 유형 enum
     */
    public enum TransactionType {
        CHARGE("CHARGE", "충전"),
        PAYMENT("PAYMENT", "결제"),
        REFUND("REFUND", "환불"),
        ADJUSTMENT("ADJUSTMENT", "조정"); // 관리자 수동 조정

        private final String code;
        private final String description;

        TransactionType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 새 이력 생성용 생성자
     */
    public BalanceHistory(Long userId, TransactionType transactionType,
            BigDecimal amount, BigDecimal balanceAfter,
            String description) {
        this.userId = userId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 거래 ID가 있는 이력 생성 (결제 시스템 연동용)
     */
    public BalanceHistory(Long userId, TransactionType transactionType,
            BigDecimal amount, BigDecimal balanceAfter,
            String description, String transactionId) {
        this(userId, transactionType, amount, balanceAfter, description);
        this.transactionId = transactionId;
    }

    /**
     * 충전 이력 생성 팩토리 메서드
     */
    public static BalanceHistory createChargeHistory(Long userId, BigDecimal amount,
            BigDecimal balanceAfter, String transactionId) {
        return new BalanceHistory(
                userId,
                TransactionType.CHARGE,
                amount,
                balanceAfter,
                String.format("잔액 충전: %s원", amount),
                transactionId);
    }

    /**
     * 결제 이력 생성 팩토리 메서드
     */
    public static BalanceHistory createPaymentHistory(Long userId, BigDecimal amount,
            BigDecimal balanceAfter, String orderId) {
        return new BalanceHistory(
                userId,
                TransactionType.PAYMENT,
                amount,
                balanceAfter,
                String.format("주문 결제: %s원 (주문번호: %s)", amount, orderId),
                orderId);
    }

    /**
     * 환불 이력 생성 팩토리 메서드
     */
    public static BalanceHistory createRefundHistory(Long userId, BigDecimal amount,
            BigDecimal balanceAfter, String orderId) {
        return new BalanceHistory(
                userId,
                TransactionType.REFUND,
                amount,
                balanceAfter,
                String.format("주문 환불: %s원 (주문번호: %s)", amount, orderId),
                orderId);
    }

    /**
     * ID 설정 (Repository에서 호출)
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 거래 유형의 한글 이름 반환
     */
    public String getTransactionTypeName() {
        return transactionType != null ? transactionType.getDescription() : "";
    }

    /**
     * 입금/출금 구분
     */
    public boolean isDeposit() {
        return transactionType == TransactionType.CHARGE ||
                transactionType == TransactionType.REFUND;
    }

    public boolean isWithdrawal() {
        return transactionType == TransactionType.PAYMENT;
    }

    /**
     * 디버깅 및 로깅용 toString
     */
    @Override
    public String toString() {
        return String.format("BalanceHistory{userId=%d, type=%s, amount=%s, balanceAfter=%s}",
                userId, transactionType, amount, balanceAfter);
    }
}