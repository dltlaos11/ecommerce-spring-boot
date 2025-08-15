package kr.hhplus.be.server.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 분산락 서비스
 * Redisson을 사용한 안전한 분산락 구현
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

    private final RedissonClient redissonClient;

    /**
     * 락 획득 시도 (타임아웃 있음)
     */
    public boolean tryLockWithTimeout(String key, long waitTime, long leaseTime) {
        RLock lock = redissonClient.getLock(key);
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
            if (acquired) {
                log.debug("분산락 획득 성공: {}", key);
            } else {
                log.warn("분산락 획득 실패 (타임아웃): {}", key);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("분산락 획득 중 인터럽트 발생: {}", key, e);
            return false;
        }
    }

    /**
     * 락 해제
     */
    public void releaseLock(String key) {
        RLock lock = redissonClient.getLock(key);
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("분산락 해제 성공: {}", key);
            } else {
                log.warn("현재 스레드가 소유하지 않은 락 해제 시도: {}", key);
            }
        } catch (Exception e) {
            log.error("분산락 해제 중 오류 발생: {}", key, e);
        }
    }

    /**
     * 락 상태 확인
     */
    public boolean isLocked(String key) {
        RLock lock = redissonClient.getLock(key);
        return lock.isLocked();
    }

    /**
     * 현재 스레드가 락을 소유하고 있는지 확인
     */
    public boolean isHeldByCurrentThread(String key) {
        RLock lock = redissonClient.getLock(key);
        return lock.isHeldByCurrentThread();
    }
}