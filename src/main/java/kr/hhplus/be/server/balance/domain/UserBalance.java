package kr.hhplus.be.server.balance.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import kr.hhplus.be.server.balance.exception.InsufficientBalanceException;
import kr.hhplus.be.server.balance.exception.InvalidChargeAmountException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 잔액 도메인 모델 (STEP05 기본 버전)
 * 
 * 설계 원칙:
 * - 잔액 관련 비즈니스 로직을 도메인 내부에 캡슐화
 * - 불변성을 보장하는 안전한 메서드 제공
 * - STEP06에서 동시성 제어 추가 예정
 * 
 * 책임:
 * - 잔액 충전/차감 로직
 * - 잔액 유효성 검증
 * - 비즈니스 규칙 적용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBalance {

    private Long id;
    private Long userId;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 잔액 관련 상수
    private static final BigDecimal MIN_CHARGE_AMOUNT = new BigDecimal("1000");
    private static final BigDecimal MAX_CHARGE_AMOUNT = new BigDecimal("1000000");
    private static final BigDecimal MAX_BALANCE_LIMIT = new BigDecimal("10000000");

    /**
     * 새 사용자 잔액 생성용 생성자
     */
    public UserBalance(Long userId) {
        this.userId = userId;
        this.balance = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 잔액 충전
     * 
     * 비즈니스 규칙:
     * - 최소/최대 충전 금액 검증
     * - 최대 보유 잔액 한도 검증
     * - 충전 후 updatedAt 자동 갱신
     * 
     * @param amount 충전할 금액
     * @throws InvalidChargeAmountException 잘못된 충전 금액
     */
    public void charge(BigDecimal amount) {
        validateChargeAmount(amount);

        BigDecimal newBalance = this.balance.add(amount);

        // 최대 보유 잔액 한도 검증
        if (newBalance.compareTo(MAX_BALANCE_LIMIT) > 0) {
            throw new InvalidChargeAmountException(ErrorCode.MAX_BALANCE_LIMIT_EXCEEDED);
        }

        this.balance = newBalance;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 잔액 차감 (결제 시 사용)
     * 
     * 비즈니스 규칙:
     * - 잔액 부족 검증
     * - 차감 후 updatedAt 자동 갱신
     * 
     * @param amount 차감할 금액
     * @throws InsufficientBalanceException 잔액 부족
     */
    public void deduct(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("차감 금액은 0보다 커야 합니다.");
        }

        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        this.balance = this.balance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
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

        // 복구 시에도 최대 한도 검증
        if (newBalance.compareTo(MAX_BALANCE_LIMIT) > 0) {
            // 복구는 예외적 상황이므로 경고 로그만 남기고 처리
            this.balance = MAX_BALANCE_LIMIT;
        } else {
            this.balance = newBalance;
        }

        this.updatedAt = LocalDateTime.now();
    }

    /**
     * ID 설정 (Repository에서 호출)
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * updatedAt 갱신 (Repository 저장 시)
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 충전 금액 유효성 검증
     */
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

    /**
     * 디버깅 및 로깅용 toString
     */
    @Override
    public String toString() {
        return String.format("UserBalance{id=%d, userId=%d, balance=%s}",
                id, userId, balance);
    }
}