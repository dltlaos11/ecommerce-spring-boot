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
        // 메서드 실행 전에 호출되므로 currentUserId가 null일 수 있음
        // 대신 현재 스레드의 실행 컨텍스트에서 파라미터를 가져와야 함
        if (currentUserId != null) {
            return new BalanceChargeLock(currentUserId).getLockKey();
        }
        // 기본값을 반환하지만, 실제로는 AOP에서 파라미터 기반으로 키를 생성해야 함
        return "ecommerce:balance:charge:default";
    }

    @Override
    public String getLockKey(Object[] methodArgs) {
        // AOP에서 메서드 파라미터 전달받아 정확한 키 생성
        if (methodArgs != null && methodArgs.length > 0 && methodArgs[0] instanceof Long) {
            Long userId = (Long) methodArgs[0];
            return new BalanceChargeLock(userId).getLockKey();
        }
        // fallback to default implementation
        return getLockKey();
    }
}