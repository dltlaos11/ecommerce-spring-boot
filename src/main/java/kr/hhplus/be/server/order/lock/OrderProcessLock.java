package kr.hhplus.be.server.order.lock;

import kr.hhplus.be.server.common.lock.Lockable;
import lombok.RequiredArgsConstructor;

/**
 * 주문 처리를 위한 분산락 키 생성
 */
@RequiredArgsConstructor
public class OrderProcessLock implements Lockable {
    
    private final Long userId;
    
    @Override
    public String getLockKey() {
        return "ecommerce:order:process:" + userId;
    }
}