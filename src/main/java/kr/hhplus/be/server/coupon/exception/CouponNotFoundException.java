package kr.hhplus.be.server.coupon.exception;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;

/**
 * 쿠폰이 없을 때 발생하는 예외
 */
public class CouponNotFoundException extends BusinessException {

    public CouponNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}