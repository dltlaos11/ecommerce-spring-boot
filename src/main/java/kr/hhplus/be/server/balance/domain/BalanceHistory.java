package kr.hhplus.be.server.balance.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ✅ 현업 스타일: Entity + Domain 통합
 */
@Entity
@Table(name = "balance_histories", indexes = {
        @Index(name = "idx_balance_histories_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_balance_histories_transaction_id", columnList = "transaction_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BalanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // 🚫 연관관계 없이 단순 FK

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", precision = 15, scale = 2, nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "description")
    private String description;

    @Column(name = "transaction_id")
    private String transactionId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 거래 유형 enum
     */
    public enum TransactionType {
        CHARGE("CHARGE", "충전"),
        PAYMENT("PAYMENT", "결제"),
        REFUND("REFUND", "환불"),
        ADJUSTMENT("ADJUSTMENT", "조정");

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

    // ======================== 생성자 ========================

    public BalanceHistory(Long userId, TransactionType transactionType,
            BigDecimal amount, BigDecimal balanceAfter,
            String description) {
        this.userId = userId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    public BalanceHistory(Long userId, TransactionType transactionType,
            BigDecimal amount, BigDecimal balanceAfter,
            String description, String transactionId) {
        this(userId, transactionType, amount, balanceAfter, description);
        this.transactionId = transactionId;
    }

    // ======================== 팩토리 메서드 (기존 Domain 로직) ========================

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

    // ======================== 비즈니스 로직 ========================

    public String getTransactionTypeName() {
        return transactionType != null ? transactionType.getDescription() : "";
    }

    public boolean isDeposit() {
        return transactionType == TransactionType.CHARGE ||
                transactionType == TransactionType.REFUND;
    }

    public boolean isWithdrawal() {
        return transactionType == TransactionType.PAYMENT;
    }

    // ======================== JPA를 위한 setter ========================

    void setId(Long id) {
        this.id = id;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("BalanceHistory{userId=%d, type=%s, amount=%s, balanceAfter=%s}",
                userId, transactionType, amount, balanceAfter);
    }
}