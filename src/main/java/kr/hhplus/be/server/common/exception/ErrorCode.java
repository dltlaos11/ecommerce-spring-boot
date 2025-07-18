package kr.hhplus.be.server.common.exception;

public enum ErrorCode {
    // 공통 에러
    INVALID_PARAMETER("INVALID_PARAMETER", "잘못된 요청 파라미터입니다."),
    VALIDATION_ERROR("VALIDATION_ERROR", "입력 값이 올바르지 않습니다."),
    NOT_FOUND("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."),
    CONFLICT("CONFLICT", "요청 처리 중 충돌이 발생했습니다."),

    // 사용자 관련
    USER_NOT_FOUND("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),

    // 잔액 관련
    INSUFFICIENT_BALANCE("INSUFFICIENT_BALANCE", "잔액이 부족합니다."),
    DAILY_CHARGE_LIMIT_EXCEEDED("DAILY_CHARGE_LIMIT_EXCEEDED", "일일 충전 한도를 초과했습니다."),
    MAX_BALANCE_LIMIT_EXCEEDED("MAX_BALANCE_LIMIT_EXCEEDED", "최대 보유 가능 잔액을 초과했습니다."),
    INVALID_CHARGE_AMOUNT("INVALID_CHARGE_AMOUNT", "충전 금액이 올바르지 않습니다."),
    BALANCE_CONCURRENCY_ERROR("BALANCE_CONCURRENCY_ERROR", "동시 처리로 인한 충돌이 발생했습니다. 다시 시도해주세요."),

    // 상품 관련
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."),
    OUT_OF_STOCK("OUT_OF_STOCK", "재고가 부족합니다."),
    PRODUCT_INACTIVE("PRODUCT_INACTIVE", "판매 중단된 상품입니다."),
    INSUFFICIENT_STOCK("INSUFFICIENT_STOCK", "재고가 부족합니다."),

    // 주문 관련
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "주문을 찾을 수 없습니다."),
    ORDER_ALREADY_PAID("ORDER_ALREADY_PAID", "이미 결제가 완료된 주문입니다."),
    ORDER_AMOUNT_MISMATCH("ORDER_AMOUNT_MISMATCH", "주문 금액이 일치하지 않습니다."),
    INVALID_ORDER_STATUS("INVALID_ORDER_STATUS", "주문 상태가 올바르지 않습니다."),
    ORDER_ITEMS_EMPTY("ORDER_ITEMS_EMPTY", "주문 상품은 최소 1개 이상이어야 합니다."),

    // 결제 관련
    PAYMENT_FAILED("PAYMENT_FAILED", "결제 처리 중 오류가 발생했습니다."),
    PAYMENT_TIMEOUT("PAYMENT_TIMEOUT", "결제 시간이 초과되었습니다."),

    // 쿠폰 관련
    COUPON_NOT_FOUND("COUPON_NOT_FOUND", "쿠폰을 찾을 수 없습니다."),
    COUPON_EXHAUSTED("COUPON_EXHAUSTED", "선착순 쿠폰이 모두 소진되었습니다."),
    COUPON_EXPIRED("COUPON_EXPIRED", "쿠폰이 만료되었습니다."),
    COUPON_ALREADY_ISSUED("COUPON_ALREADY_ISSUED", "이미 발급받은 쿠폰입니다."),
    COUPON_ALREADY_USED("COUPON_ALREADY_USED", "이미 사용된 쿠폰입니다."),
    MINIMUM_ORDER_AMOUNT_NOT_MET("MINIMUM_ORDER_AMOUNT_NOT_MET", "최소 주문 금액을 만족하지 않습니다."),
    COUPON_NOT_APPLICABLE("COUPON_NOT_APPLICABLE", "적용할 수 없는 쿠폰입니다.");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}