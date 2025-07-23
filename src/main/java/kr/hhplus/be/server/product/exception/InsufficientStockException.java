package kr.hhplus.be.server.product.exception;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;

/**
 * 재고가 부족할 때 발생하는 예외
 */
public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InsufficientStockException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}