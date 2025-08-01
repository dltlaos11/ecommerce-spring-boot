package kr.hhplus.be.server.order.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entity + Domain 통합
 * 연관관계 매핑 제거 - Repository 2번 호출 방식 사용
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_order_id", columnList = "order_id", unique = true),
        @Index(name = "idx_payments_user_created", columnList = "user_id, created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId; // 연관관계 없이 단순 FK

    @Column(name = "user_id", nullable = false)
    private Long userId; // 연관관계 없이 단순 FK

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
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

    // 생성자

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
    }

    // 비즈니스 로직 (기존 Domain 로직 그대로)

    /**
     * 결제 완료 처리
     */
    public void complete() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("대기 중인 결제만 완료할 수 있습니다.");
        }
        this.status = PaymentStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 결제 실패 처리
     */
    public void fail() {
        if (this.status == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("완료된 결제는 실패 처리할 수 없습니다.");
        }
        this.status = PaymentStatus.FAILED;
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

    // JPA를 위한 setter

    void setId(Long id) {
        this.id = id;
    }

    void setStatus(PaymentStatus status) {
        this.status = status;
    }

    void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // 비즈니스 로직 헬퍼

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