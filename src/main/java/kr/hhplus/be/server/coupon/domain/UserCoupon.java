package kr.hhplus.be.server.coupon.domain;

import java.time.LocalDateTime;

import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.coupon.exception.CouponAlreadyUsedException;
import kr.hhplus.be.server.coupon.exception.CouponExpiredException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 쿠폰 도메인 모델
 * 
 * 설계 원칙:
 * - 사용자별 쿠폰 발급 및 사용 이력 관리
 * - 쿠폰 상태 변경 로직 캡슐화
 * - 사용 조건 검증 로직 내장
 * 
 * 책임:
 * - 쿠폰 사용 처리
 * - 쿠폰 상태 관리 (AVAILABLE, USED, EXPIRED)
 * - 사용 가능 여부 검증
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCoupon {

    private Long id;
    private Long userId;
    private Long couponId;
    private CouponStatus status;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
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

    /**
     * 새 사용자 쿠폰 생성용 생성자
     */
    public UserCoupon(Long userId, Long couponId) {
        this.userId = userId;
        this.couponId = couponId;
        this.status = CouponStatus.AVAILABLE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰 사용 처리
     * 
     * 🎯 비즈니스 규칙:
     * - 사용 가능한 상태여야 함
     * - 이미 사용된 쿠폰은 재사용 불가
     * - 만료된 쿠폰은 사용 불가
     * 
     * @throws CouponAlreadyUsedException 이미 사용된 쿠폰인 경우
     * @throws CouponExpiredException     만료된 쿠폰인 경우
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
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰 만료 처리
     */
    public void expire() {
        if (status == CouponStatus.AVAILABLE) {
            this.status = CouponStatus.EXPIRED;
            this.updatedAt = LocalDateTime.now();
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
    public void setStatus(CouponStatus status) {
        this.status = status;
    }

    /**
     * 사용 시간 설정 (Repository에서 호출)
     */
    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    /**
     * 디버깅 및 로깅용 toString
     */
    @Override
    public String toString() {
        return String.format("UserCoupon{id=%d, userId=%d, couponId=%d, status=%s}",
                id, userId, couponId, status);
    }
}