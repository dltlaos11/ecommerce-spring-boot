package kr.hhplus.be.server.coupon.exception;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;

/**
 * 이미 발급받은 쿠폰일 때 발생하는 예외
 */
public class CouponAlreadyIssuedException extends BusinessException {

    public CouponAlreadyIssuedException(ErrorCode errorCode) {
        super(errorCode);
    }
}