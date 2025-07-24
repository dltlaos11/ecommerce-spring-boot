package kr.hhplus.be.server.balance.exception;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;

/**
 * 잔액이 부족할 때 발생하는 예외
 */
public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException(ErrorCode errorCode) {
        super(errorCode);
    }
}