package kr.hhplus.be.server.coupon.exception;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;

/**
 * 쿠폰이 모두 소진되었을 때 발생하는 예외
 */
public class CouponExhaustedException extends BusinessException {

    public CouponExhaustedException(ErrorCode errorCode) {
        super(errorCode);
    }
}