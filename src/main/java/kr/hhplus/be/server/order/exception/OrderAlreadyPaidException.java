package kr.hhplus.be.server.order.exception;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;

/**
 * 이미 결제가 완료된 주문일 때 발생하는 예외
 */
public class OrderAlreadyPaidException extends BusinessException {

    public OrderAlreadyPaidException(ErrorCode errorCode) {
        super(errorCode);
    }
}