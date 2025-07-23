package kr.hhplus.be.server.balance.exception;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;

/**
 * 잘못된 충전 금액일 때 발생하는 예외
 */
public class InvalidChargeAmountException extends BusinessException {

    public InvalidChargeAmountException(ErrorCode errorCode) {
        super(errorCode);
    }
}