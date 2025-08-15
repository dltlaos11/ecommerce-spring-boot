package kr.hhplus.be.server.common.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 분산락 어노테이션
 * AOP를 통해 메서드 실행 전후로 분산락을 획득/해제한다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    
    /**
     * 락 키
     * 인터페이스 기반 방식을 권장 (Spring EL 방식 지양)
     */
    String key();
    
    /**
     * 락 대기 시간 (밀리초)
     * 기본값: 5초
     */
    long waitTime() default 5000L;
    
    /**
     * 락 리스 시간 (밀리초) 
     * 기본값: 10초
     */
    long leaseTime() default 10000L;
}