// UserCoupon Infrastructure JPA Repository
package kr.hhplus.be.server.coupon.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.hhplus.be.server.coupon.domain.UserCoupon;

/**
 * UserCoupon JPA Repository (Infrastructure Layer)
 */
public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {

    /**
     * 사용자별 쿠폰 목록 조회
     */
    List<UserCoupon> findByUserId(Long userId);

    /**
     * 사용자별 특정 쿠폰 보유 여부 확인 (중복 발급 방지용)
     */
    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);

    /**
     * 사용자별 사용 가능한 쿠폰 목록 조회
     */
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.status = 'AVAILABLE'")
    List<UserCoupon> findAvailableCouponsByUserId(@Param("userId") Long userId);
}