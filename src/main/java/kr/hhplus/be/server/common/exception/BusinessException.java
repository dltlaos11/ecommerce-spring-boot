package kr.hhplus.be.server.common.exception;

/**
 * 비즈니스 예외의 기본 클래스
 * 
 * - 모든 비즈니스 예외의 부모 클래스
 * - ErrorCode enum과 연동하여 일관된 예외 처리
 * - RuntimeException을 상속하여 Checked Exception 부담 제거
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}