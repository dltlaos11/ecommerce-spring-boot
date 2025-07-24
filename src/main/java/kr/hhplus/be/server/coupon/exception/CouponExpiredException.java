package kr.hhplus.be.server.coupon.exception;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;

/**
 * 쿠폰이 만료되었을 때 발생하는 예외
 */
public class CouponExpiredException extends BusinessException {

    public CouponExpiredException(ErrorCode errorCode) {
        super(errorCode);
    }
}