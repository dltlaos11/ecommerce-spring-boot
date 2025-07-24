package kr.hhplus.be.server.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import kr.hhplus.be.server.common.response.CommonResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 전역 예외 처리기
 * 
 * 설계 원칙:
 * - 모든 예외를 CommonResponse 형태로 통일
 * - 비즈니스 예외와 시스템 예외 구분 처리
 * - 적절한 HTTP 상태 코드 반환
 * - 보안을 위해 내부 에러 정보 노출 최소화
 * 
 * 책임:
 * - BusinessException 처리
 * - Validation 예외 처리
 * - 시스템 예외 처리
 * - 에러 로깅
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     * 
     * 잔액 부족, 상품 없음, 쿠폰 소진 등 비즈니스 로직에서 발생하는 예외
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("🚨 비즈니스 예외 발생: {} - {}", e.getErrorCode().getCode(), e.getMessage());

        CommonResponse<Void> response = CommonResponse.error(
                e.getErrorCode().getCode(),
                e.getErrorCode().getMessage());

        HttpStatus status = determineHttpStatus(e.getErrorCode());
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Validation 예외 처리
     * 
     * @Valid 어노테이션으로 검증 실패 시 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("🚨 요청 데이터 검증 실패: {}", e.getMessage());

        // 첫 번째 필드 에러 메시지 추출
        String errorMessage = "입력 값이 올바르지 않습니다.";
        if (e.getBindingResult().hasErrors()) {
            FieldError fieldError = e.getBindingResult().getFieldErrors().get(0);
            errorMessage = fieldError.getDefaultMessage();
        }

        CommonResponse<Void> response = CommonResponse.error(
                ErrorCode.VALIDATION_ERROR.getCode(),
                errorMessage);

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * IllegalArgumentException 처리
     * 
     * 잘못된 파라미터 등으로 발생하는 예외
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("🚨 잘못된 파라미터: {}", e.getMessage());

        CommonResponse<Void> response = CommonResponse.error(
                ErrorCode.INVALID_PARAMETER.getCode(),
                e.getMessage());

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 시스템 예외 처리
     * 
     * 예상하지 못한 모든 예외 처리 (보안상 상세 정보 노출 금지)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleException(Exception e) {
        log.error("🔥 예상치 못한 시스템 오류 발생", e);

        CommonResponse<Void> response = CommonResponse.error(
                ErrorCode.INTERNAL_ERROR.getCode(),
                ErrorCode.INTERNAL_ERROR.getMessage());

        return ResponseEntity.internalServerError().body(response);
    }

    /**
     * ErrorCode에 따른 HTTP 상태 코드 결정
     */
    private HttpStatus determineHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            // 400 Bad Request
            case INVALID_PARAMETER, VALIDATION_ERROR,
                    INVALID_CHARGE_AMOUNT, INSUFFICIENT_BALANCE,
                    INSUFFICIENT_STOCK, OUT_OF_STOCK,
                    MINIMUM_ORDER_AMOUNT_NOT_MET, ORDER_ITEMS_EMPTY ->
                HttpStatus.BAD_REQUEST;

            // 404 Not Found
            case USER_NOT_FOUND, PRODUCT_NOT_FOUND, ORDER_NOT_FOUND,
                    COUPON_NOT_FOUND, NOT_FOUND ->
                HttpStatus.NOT_FOUND;

            // 409 Conflict
            case BALANCE_CONCURRENCY_ERROR, COUPON_EXHAUSTED,
                    COUPON_ALREADY_ISSUED, COUPON_ALREADY_USED,
                    ORDER_ALREADY_PAID, CONFLICT ->
                HttpStatus.CONFLICT;

            // 429 Too Many Requests (한도 초과)
            case DAILY_CHARGE_LIMIT_EXCEEDED, MAX_BALANCE_LIMIT_EXCEEDED ->
                HttpStatus.TOO_MANY_REQUESTS;

            // 500 Internal Server Error
            case INTERNAL_ERROR, PAYMENT_FAILED, PAYMENT_TIMEOUT ->
                HttpStatus.INTERNAL_SERVER_ERROR;

            // 기본값: 400 Bad Request
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}