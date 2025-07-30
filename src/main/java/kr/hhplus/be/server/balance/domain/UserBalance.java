package kr.hhplus.be.server.balance.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import kr.hhplus.be.server.balance.exception.InsufficientBalanceException;
import kr.hhplus.be.server.balance.exception.InvalidChargeAmountException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ✅ 현업 스타일: Entity + Domain 통합
 * 
 * 장점:
 * - 불필요한 변환 로직 제거
 * - 코드 중복 제거
 * - 유지보수 비용 최소화
 * - 팀 전체 이해도 향상
 */
@Entity
@Table(name = "user_balances", indexes = {
        @Index(name = "idx_user_balances_user_id", columnList = "user_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Version // 🔒 낙관적 락
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 잔액 관련 상수
    private static final BigDecimal MIN_CHARGE_AMOUNT = new BigDecimal("1000");
    private static final BigDecimal MAX_CHARGE_AMOUNT = new BigDecimal("1000000");
    private static final BigDecimal MAX_BALANCE_LIMIT = new BigDecimal("10000000");

    // ======================== 생성자 ========================

    /**
     * 새 사용자 잔액 생성용 생성자
     */
    public UserBalance(Long userId) {
        this.userId = userId;
        this.balance = BigDecimal.ZERO;
    }

    // ======================== 비즈니스 로직 (기존 Domain 로직 그대로) ========================

    /**
     * 잔액 충전
     */
    public void charge(BigDecimal amount) {
        validateChargeAmount(amount);

        BigDecimal newBalance = this.balance.add(amount);

        if (newBalance.compareTo(MAX_BALANCE_LIMIT) > 0) {
            throw new InvalidChargeAmountException(ErrorCode.MAX_BALANCE_LIMIT_EXCEEDED);
        }

        this.balance = newBalance;
        // updatedAt은 @LastModifiedDate가 자동 처리
    }

    /**
     * 잔액 차감
     */
    public void deduct(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("차감 금액은 0보다 커야 합니다.");
        }

        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        this.balance = this.balance.subtract(amount);
    }

    /**
     * 잔액 충분 여부 확인
     */
    public boolean hasEnoughBalance(BigDecimal amount) {
        if (amount == null)
            return false;
        return this.balance.compareTo(amount) >= 0;
    }

    /**
     * 잔액 복구 (결제 실패 시)
     */
    public void refund(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("복구 금액은 0보다 커야 합니다.");
        }

        BigDecimal newBalance = this.balance.add(amount);

        if (newBalance.compareTo(MAX_BALANCE_LIMIT) > 0) {
            this.balance = MAX_BALANCE_LIMIT;
        } else {
            this.balance = newBalance;
        }
    }

    // ======================== JPA를 위한 setter (package-private)
    // ========================

    void setId(Long id) {
        this.id = id;
    }

    void setUserId(Long userId) {
        this.userId = userId;
    }

    void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    void setVersion(Long version) {
        this.version = version;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ======================== 비즈니스 로직 헬퍼 ========================

    private void validateChargeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidChargeAmountException(ErrorCode.INVALID_CHARGE_AMOUNT);
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidChargeAmountException(ErrorCode.INVALID_CHARGE_AMOUNT);
        }

        if (amount.compareTo(MIN_CHARGE_AMOUNT) < 0) {
            throw new InvalidChargeAmountException(ErrorCode.INVALID_CHARGE_AMOUNT);
        }

        if (amount.compareTo(MAX_CHARGE_AMOUNT) > 0) {
            throw new InvalidChargeAmountException(ErrorCode.INVALID_CHARGE_AMOUNT);
        }
    }

    @Override
    public String toString() {
        return String.format("UserBalance{id=%d, userId=%d, balance=%s}",
                id, userId, balance);
    }
}