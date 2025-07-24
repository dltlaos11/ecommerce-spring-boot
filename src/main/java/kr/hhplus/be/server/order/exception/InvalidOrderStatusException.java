package kr.hhplus.be.server.order.exception;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;

/**
 * 주문 상태가 올바르지 않을 때 발생하는 예외
 */
public class InvalidOrderStatusException extends BusinessException {

    public InvalidOrderStatusException(ErrorCode errorCode) {
        super(errorCode);
    }
}