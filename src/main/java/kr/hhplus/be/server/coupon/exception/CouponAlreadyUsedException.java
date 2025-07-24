package kr.hhplus.be.server.coupon.exception;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;

/**
 * 이미 사용된 쿠폰일 때 발생하는 예외
 */
public class CouponAlreadyUsedException extends BusinessException {

    public CouponAlreadyUsedException(ErrorCode errorCode) {
        super(errorCode);
    }
}