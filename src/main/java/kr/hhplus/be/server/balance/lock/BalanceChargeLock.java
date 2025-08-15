package kr.hhplus.be.server.balance.lock;

import kr.hhplus.be.server.common.lock.Lockable;
import lombok.RequiredArgsConstructor;

/**
 * 잔액 충전을 위한 분산락 키 생성
 */
@RequiredArgsConstructor
public class BalanceChargeLock implements Lockable {
    
    private final Long userId;
    
    @Override
    public String getLockKey() {
        return "ecommerce:balance:charge:" + userId;
    }
}