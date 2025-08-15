package kr.hhplus.be.server.common.lock;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;

/**
 * 분산락 관련 예외
 */
public class DistributedLockException extends BusinessException {
    
    public DistributedLockException(String message) {
        super(ErrorCode.INTERNAL_ERROR, message);
    }
    
    public DistributedLockException(String message, Throwable cause) {
        super(ErrorCode.INTERNAL_ERROR, message, cause);
    }
}