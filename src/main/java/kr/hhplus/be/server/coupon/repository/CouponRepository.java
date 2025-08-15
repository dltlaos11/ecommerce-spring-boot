package kr.hhplus.be.server.coupon.repository;

import java.util.List;
import java.util.Optional;

import kr.hhplus.be.server.coupon.domain.Coupon;

/**
 * 쿠폰 저장소 인터페이스 (Domain Layer)
 * N+1 문제 해결을 위한 배치 조회 메서드 추가
 */
public interface CouponRepository {

    /**
     * ID로 쿠폰 조회
     */
    Optional<Coupon> findById(Long id);

    /**
     * 모든 쿠폰 목록 조회
     */
    List<Coupon> findAll();

    /**
     * 여러 ID로 쿠폰 목록 조회 (N+1 문제 해결용)
     * 
     * @param ids 쿠폰 ID 목록
     * @return 해당 ID들의 쿠폰 목록
     */
    List<Coupon> findAllById(Iterable<Long> ids);

    /**
     * 발급 가능한 쿠폰 목록 조회
     */
    List<Coupon> findAvailableCoupons();

    /**
     * 쿠폰 저장 (생성 또는 수정)
     */
    Coupon save(Coupon coupon);

    /**
     * 쿠폰 삭제
     */
    void delete(Coupon coupon);

    /**
     * ID로 쿠폰 삭제
     */
    void deleteById(Long id);

    // 비관적 락 메서드 제거 - 분산락으로 대체
}