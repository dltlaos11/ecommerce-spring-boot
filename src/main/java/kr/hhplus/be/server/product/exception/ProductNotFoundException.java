package kr.hhplus.be.server.product.exception;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;

/**
 * 상품을 찾을 수 없을 때 발생하는 예외
 */
public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ProductNotFoundException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}