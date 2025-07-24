package kr.hhplus.be.server.order.exception;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;

/**
 * 주문 금액이 일치하지 않을 때 발생하는 예외
 */
public class OrderAmountMismatchException extends BusinessException {

    public OrderAmountMismatchException(ErrorCode errorCode) {
        super(errorCode);
    }
}