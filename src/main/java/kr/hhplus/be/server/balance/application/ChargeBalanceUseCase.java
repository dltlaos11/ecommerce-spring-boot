package kr.hhplus.be.server.balance.application;

import java.math.BigDecimal;


import kr.hhplus.be.server.balance.dto.ChargeBalanceResponse;
import kr.hhplus.be.server.balance.lock.BalanceChargeLock;
import kr.hhplus.be.server.balance.service.BalanceService;
import kr.hhplus.be.server.common.annotation.UseCase;
import kr.hhplus.be.server.common.lock.DistributedLock;
import kr.hhplus.be.server.common.lock.Lockable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 잔액 충전 UseCase - 단일 비즈니스 요구사항만 처리
 * 
 * 하나의 구체적인 요구사항: "사용자가 잔액을 충전한다"
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
public class ChargeBalanceUseCase implements Lockable {

    private final BalanceService balanceService;
    
    private Long currentUserId; // 현재 처리 중인 사용자 ID

    @DistributedLock(key = "ecommerce:balance:charge", waitTime = 3000, leaseTime = 5000)
    public ChargeBalanceResponse execute(Long userId, BigDecimal amount) {
        this.currentUserId = userId;
        return balanceService.chargeBalance(userId, amount);
    }
    
    @Override
    public String getLockKey() {
        if (currentUserId != null) {
            return new BalanceChargeLock(currentUserId).getLockKey();
        }
        return "ecommerce:balance:charge:default";
    }
}