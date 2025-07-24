package kr.hhplus.be.server.order.exception;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;

/**
 * 결제 처리 중 오류가 발생했을 때의 예외
 */
public class PaymentFailedException extends BusinessException {

    public PaymentFailedException(ErrorCode errorCode) {
        super(errorCode);
    }
}