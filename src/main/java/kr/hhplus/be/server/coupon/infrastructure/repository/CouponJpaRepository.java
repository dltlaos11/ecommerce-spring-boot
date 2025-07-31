package kr.hhplus.be.server.coupon.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.coupon.domain.Coupon;

/**
 * Coupon JPA Repository (Infrastructure Layer)
 */
public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {

    /**
     * 발급 가능한 쿠폰 목록 조회
     * 만료되지 않고 수량이 남은 쿠폰만
     */
    @Query("""
            SELECT c FROM Coupon c
            WHERE c.expiredAt > :now
            AND c.issuedQuantity < c.totalQuantity
            ORDER BY c.createdAt DESC
            """)
    List<Coupon> findAvailableCoupons(@Param("now") LocalDateTime now);

    /**
     * 비관적 락으로 쿠폰 조회 (선착순 발급용)
     * 🔒 SELECT FOR UPDATE
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    Optional<Coupon> findByIdForUpdate(@Param("id") Long id);
}
