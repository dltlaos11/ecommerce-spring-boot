package kr.hhplus.be.server.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 분산락 AOP
 * @DistributedLock 어노테이션이 적용된 메서드에 대해 분산락을 적용한다.
 * 
 * 실행 순서: 분산락 획득 → 트랜잭션 시작 → 비즈니스 로직 → 트랜잭션 커밋 → 분산락 해제
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private final DistributedLockService lockService;

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = generateLockKey(joinPoint, distributedLock);
        
        boolean acquired = false;
        try {
            // 분산락 획득 시도
            acquired = lockService.tryLockWithTimeout(
                lockKey,
                distributedLock.waitTime(),
                distributedLock.leaseTime()
            );
            
            if (!acquired) {
                throw new DistributedLockException("분산락 획득 실패: " + lockKey);
            }
            
            log.debug("분산락 AOP: 락 획득 성공, 비즈니스 로직 실행 시작 - {}", lockKey);
            
            // 비즈니스 로직 실행
            return joinPoint.proceed();
            
        } finally {
            if (acquired) {
                lockService.releaseLock(lockKey);
                log.debug("분산락 AOP: 락 해제 완료 - {}", lockKey);
            }
        }
    }
    
    /**
     * 락 키 생성
     * 인터페이스 기반 방식 우선, 아니면 고정 키 사용
     */
    private String generateLockKey(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
        Object target = joinPoint.getTarget();
        
        // Lockable 인터페이스 구현체인 경우
        if (target instanceof Lockable) {
            // 메서드 파라미터를 전달하여 정확한 키 생성
            Object[] methodArgs = joinPoint.getArgs();
            return ((Lockable) target).getLockKey(methodArgs);
        }
        
        // 어노테이션의 key 값 사용
        return distributedLock.key();
    }
}