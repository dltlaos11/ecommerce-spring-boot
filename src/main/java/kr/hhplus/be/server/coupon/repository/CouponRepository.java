package kr.hhplus.be.server.coupon.repository;

import java.util.List;
import java.util.Optional;

import kr.hhplus.be.server.coupon.domain.Coupon;

/**
 * 쿠폰 저장소 인터페이스
 * 
 * 설계 원칙:
 * - DIP(의존성 역전 원칙) 적용
 * - 쿠폰 발급 시 동시성 제어 지원 (STEP06에서 활용)
 * - 테스트 시 Mock으로 쉽게 대체 가능
 * 
 * 책임:
 * - 쿠폰 CRUD 기본 연산
 * - 발급 가능한 쿠폰 조회
 * - 쿠폰 발급 처리 (수량 관리)
 */
public interface CouponRepository {

    /**
     * ID로 쿠폰 조회
     * 
     * @param id 쿠폰 ID
     * @return 쿠폰 정보 (Optional로 null 안전성 보장)
     */
    Optional<Coupon> findById(Long id);

    /**
     * 모든 쿠폰 목록 조회
     * 
     * @return 전체 쿠폰 목록
     */
    List<Coupon> findAll();

    /**
     * 발급 가능한 쿠폰 목록 조회
     * 
     * @return 만료되지 않고 수량이 남은 쿠폰 목록
     */
    List<Coupon> findAvailableCoupons();

    /**
     * 쿠폰 저장 (생성 또는 수정)
     * 
     * @param coupon 저장할 쿠폰
     * @return 저장된 쿠폰 (ID가 할당된 상태)
     */
    Coupon save(Coupon coupon);

    /**
     * 쿠폰 삭제
     * 
     * @param coupon 삭제할 쿠폰
     */
    void delete(Coupon coupon);

    /**
     * ID로 쿠폰 삭제
     * 
     * @param id 삭제할 쿠폰 ID
     */
    void deleteById(Long id);

    /**
     * 비관적 락으로 쿠폰 조회 (선착순 발급용 - STEP06에서 활용)
     * 
     * 🔒 STEP06에서 활용:
     * - 선착순 쿠폰 발급 시 동시성 문제 해결
     * - 여러 사용자가 동시에 같은 쿠폰을 요청할 때 정확한 수량 관리
     * 
     * @param id 쿠폰 ID
     * @return 락이 걸린 쿠폰 정보
     */
    Optional<Coupon> findByIdForUpdate(Long id);
}