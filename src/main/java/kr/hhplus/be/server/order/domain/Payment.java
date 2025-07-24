package kr.hhplus.be.server.order.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 결제 도메인 모델
 * 
 * 설계 원칙:
 * - 결제 정보 및 상태 관리
 * - 결제 방법별 처리 로직 추상화
 * - 결제 이력 추적
 * 
 * 책임:
 * - 결제 상태 변경 로직
 * - 결제 성공/실패 처리
 * - 결제 금액 검증
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    private Long id;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 결제 방법 enum
     */
    public enum PaymentMethod {
        BALANCE("BALANCE", "잔액 결제"),
        CARD("CARD", "카드 결제"),
        BANK_TRANSFER("BANK_TRANSFER", "계좌 이체");

        private final String code;
        private final String description;

        PaymentMethod(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 결제 상태 enum
     */
    public enum PaymentStatus {
        PENDING("PENDING", "결제 대기"),
        COMPLETED("COMPLETED", "결제 완료"),
        FAILED("FAILED", "결제 실패"),
        CANCELLED("CANCELLED", "결제 취소");

        private final String code;
        private final String description;

        PaymentStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 새 결제 생성용 생성자
     */
    public Payment(Long orderId, Long userId, BigDecimal amount, PaymentMethod paymentMethod) {
        validateInputs(orderId, userId, amount, paymentMethod);

        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 결제 완료 처리
     */
    public void complete() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("대기 중인 결제만 완료할 수 있습니다.");
        }
        this.status = PaymentStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 결제 실패 처리
     */
    public void fail() {
        if (this.status == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("완료된 결제는 실패 처리할 수 없습니다.");
        }
        this.status = PaymentStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 결제 취소 처리
     */
    public void cancel() {
        if (this.status == PaymentStatus.COMPLETED) {
            // 완료된 결제는 환불로 처리
            this.status = PaymentStatus.CANCELLED;
        } else {
            // 대기 중인 결제는 취소로 처리
            this.status = PaymentStatus.CANCELLED;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 결제 완료 여부
     */
    public boolean isCompleted() {
        return this.status == PaymentStatus.COMPLETED;
    }

    /**
     * 결제 실패 여부
     */
    public boolean isFailed() {
        return this.status == PaymentStatus.FAILED;
    }

    /**
     * ID 설정 (Repository에서 호출)
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * updatedAt 갱신 (Repository 저장 시)
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 상태 설정 (Repository에서 호출)
     */
    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    /**
     * 처리 시간 설정 (Repository에서 호출)
     */
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    /**
     * 입력값 검증
     */
    private void validateInputs(Long orderId, Long userId, BigDecimal amount, PaymentMethod paymentMethod) {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다.");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("결제 방법은 필수입니다.");
        }
    }

    /**
     * 디버깅 및 로깅용 toString
     */
    @Override
    public String toString() {
        return String.format("Payment{id=%d, orderId=%d, amount=%s, method=%s, status=%s}",
                id, orderId, amount, paymentMethod, status);
    }
}