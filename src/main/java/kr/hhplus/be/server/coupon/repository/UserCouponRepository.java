package kr.hhplus.be.server.coupon.repository;

import java.util.List;
import java.util.Optional;

import kr.hhplus.be.server.coupon.domain.UserCoupon;

/**
 * 사용자 쿠폰 저장소 인터페이스
 * 
 * 설계 원칙:
 * - DIP(의존성 역전 원칙) 적용
 * - 중복 발급 방지를 위한 조회 기능 제공
 * - 테스트 시 Mock으로 쉽게 대체 가능
 * 
 * 책임:
 * - 사용자 쿠폰 CRUD
 * - 중복 발급 검증 지원
 * - 사용자별 쿠폰 목록 조회
 */
public interface UserCouponRepository {

    /**
     * ID로 사용자 쿠폰 조회
     * 
     * @param id 사용자 쿠폰 ID
     * @return 사용자 쿠폰 정보
     */
    Optional<UserCoupon> findById(Long id);

    /**
     * 사용자별 쿠폰 목록 조회
     * 
     * @param userId 사용자 ID
     * @return 사용자가 보유한 쿠폰 목록
     */
    List<UserCoupon> findByUserId(Long userId);

    /**
     * 사용자별 특정 쿠폰 보유 여부 확인 (중복 발급 방지용)
     * 
     * @param userId   사용자 ID
     * @param couponId 쿠폰 ID
     * @return 해당 쿠폰 보유 정보 (없으면 Empty)
     */
    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);

    /**
     * 사용자별 사용 가능한 쿠폰 목록 조회
     * 
     * @param userId 사용자 ID
     * @return 사용 가능한 쿠폰 목록 (AVAILABLE 상태)
     */
    List<UserCoupon> findAvailableCouponsByUserId(Long userId);

    /**
     * 사용자 쿠폰 저장 (생성 또는 수정)
     * 
     * @param userCoupon 저장할 사용자 쿠폰
     * @return 저장된 사용자 쿠폰 (ID가 할당된 상태)
     */
    UserCoupon save(UserCoupon userCoupon);

    /**
     * 사용자 쿠폰 삭제
     * 
     * @param userCoupon 삭제할 사용자 쿠폰
     */
    void delete(UserCoupon userCoupon);

    /**
     * ID로 사용자 쿠폰 삭제
     * 
     * @param id 삭제할 사용자 쿠폰 ID
     */
    void deleteById(Long id);

    /**
     * 모든 사용자 쿠폰 조회 (관리자용)
     * 
     * @return 전체 사용자 쿠폰 목록
     */
    List<UserCoupon> findAll();
}