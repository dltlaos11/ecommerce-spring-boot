package kr.hhplus.be.server.balance.exception;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;

/**
 * 잔액 동시성 처리 중 충돌이 발생했을 때의 예외
 */
public class BalanceConcurrencyException extends BusinessException {

    public BalanceConcurrencyException(ErrorCode errorCode) {
        super(errorCode);
    }
}