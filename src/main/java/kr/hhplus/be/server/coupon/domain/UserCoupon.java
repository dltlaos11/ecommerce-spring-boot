package kr.hhplus.be.server.coupon.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.coupon.exception.CouponAlreadyUsedException;
import kr.hhplus.be.server.coupon.exception.CouponExpiredException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entity + Domain 통합
 * Repository 2번 호출 방식 사용
 */
@Entity
@Table(name = "user_coupons", indexes = {
        @Index(name = "idx_user_coupon_unique", columnList = "user_id, coupon_id", unique = true),
        @Index(name = "idx_user_coupons_user_status", columnList = "user_id, status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // 🚫 연관관계 없이 단순 FK

    @Column(name = "coupon_id", nullable = false)
    private Long couponId; // 🚫 연관관계 없이 단순 FK

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CouponStatus status = CouponStatus.AVAILABLE;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 쿠폰 상태 enum
     */
    public enum CouponStatus {
        AVAILABLE("AVAILABLE", "사용 가능"),
        USED("USED", "사용 완료"),
        EXPIRED("EXPIRED", "만료됨");

        private final String code;
        private final String description;

        CouponStatus(String code, String description) {
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
     * 새 사용자 쿠폰 생성용 생성자
     */
    public UserCoupon(Long userId, Long couponId) {
        this.userId = userId;
        this.couponId = couponId;
        this.status = CouponStatus.AVAILABLE;
    }

    // 비즈니스 로직 (기존 Domain 로직 그대로)

    /**
     * 쿠폰 사용 처리
     */
    public void use() {
        if (status == CouponStatus.USED) {
            throw new CouponAlreadyUsedException(ErrorCode.COUPON_ALREADY_USED);
        }

        if (status == CouponStatus.EXPIRED) {
            throw new CouponExpiredException(ErrorCode.COUPON_EXPIRED);
        }

        this.status = CouponStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰 만료 처리
     */
    public void expire() {
        if (status == CouponStatus.AVAILABLE) {
            this.status = CouponStatus.EXPIRED;
        }
    }

    /**
     * 사용 가능 여부 확인
     */
    public boolean isUsable() {
        return status == CouponStatus.AVAILABLE;
    }

    /**
     * 사용 완료 여부 확인
     */
    public boolean isUsed() {
        return status == CouponStatus.USED;
    }

    /**
     * 만료 여부 확인
     */
    public boolean isExpired() {
        return status == CouponStatus.EXPIRED;
    }

    // JPA를 위한 setter

    void setId(Long id) {
        this.id = id;
    }

    void setStatus(CouponStatus status) {
        this.status = status;
    }

    void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ========== UserCoupon.java 추가 ==========
    /**
     * 테스트 전용 setter 메서드들
     * 
     * @deprecated 테스트에서만 사용
     */
    @Deprecated
    public void setIdForTest(Long id) {
        this.id = id;
    }

    @Deprecated
    public void setCreatedAtForTest(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Deprecated
    public void setUpdatedAtForTest(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return String.format("UserCoupon{id=%d, userId=%d, couponId=%d, status=%s}",
                id, userId, couponId, status);
    }
}