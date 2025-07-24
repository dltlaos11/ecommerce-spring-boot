package kr.hhplus.be.server.order.exception;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;

/**
 * 주문을 찾을 수 없을 때 발생하는 예외
 */
public class OrderNotFoundException extends BusinessException {

    public OrderNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}