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
     * 거래 유형 enum - code 필드 제거, Enum.name() 활용
     */
    public enum TransactionType {
        CHARGE("충전"),
        PAYMENT("결제"),
        REFUND("환불"),
        ADJUSTMENT("조정");

        private final String description;

        TransactionType(String description) {
            this.description = description;
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
                "잔액 충전: " + amount + "원",
                transactionId);
    }

    public static BalanceHistory createPaymentHistory(Long userId, BigDecimal amount,
            BigDecimal balanceAfter, String orderId) {
        return new BalanceHistory(
                userId,
                TransactionType.PAYMENT,
                amount,
                balanceAfter,
                "주문 결제: " + amount + "원 (주문번호: " + orderId + ")",
                orderId);
    }

    public static BalanceHistory createRefundHistory(Long userId, BigDecimal amount,
            BigDecimal balanceAfter, String orderId) {
        return new BalanceHistory(
                userId,
                TransactionType.REFUND,
                amount,
                balanceAfter,
                "주문 환불: " + amount + "원 (주문번호: " + orderId + ")",
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
        return "BalanceHistory{userId=" + userId + ", type=" + transactionType + 
                ", amount=" + amount + ", balanceAfter=" + balanceAfter + "}";
    }

    // ======================== 테스트를 위한 생성자 및 setter ========================

    /**
     * 테스트 전용 - ID 설정
     * 
     * @deprecated 테스트에서만 사용
     */
    @Deprecated
    public void setIdForTest(Long id) {
        this.id = id;
    }

    /**
     * 테스트 전용 - 사용자 ID 설정
     * 
     * @deprecated 테스트에서만 사용
     */
    @Deprecated
    public void setUserIdForTest(Long userId) {
        this.userId = userId;
    }

    /**
     * 테스트 전용 - 거래 유형 설정
     * 
     * @deprecated 테스트에서만 사용
     */
    @Deprecated
    public void setTransactionTypeForTest(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * 테스트 전용 - 금액 설정
     * 
     * @deprecated 테스트에서만 사용
     */
    @Deprecated
    public void setAmountForTest(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * 테스트 전용 - 거래 후 잔액 설정
     * 
     * @deprecated 테스트에서만 사용
     */
    @Deprecated
    public void setBalanceAfterForTest(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    /**
     * 테스트 전용 - 설명 설정
     * 
     * @deprecated 테스트에서만 사용
     */
    @Deprecated
    public void setDescriptionForTest(String description) {
        this.description = description;
    }

    /**
     * 테스트 전용 - 생성시간 설정
     * 
     * @deprecated 테스트에서만 사용
     */
    @Deprecated
    public void setCreatedAtForTest(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}