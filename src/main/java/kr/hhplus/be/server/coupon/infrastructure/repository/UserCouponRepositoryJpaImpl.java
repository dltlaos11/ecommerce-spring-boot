package kr.hhplus.be.server.coupon.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ✅ 현업 방식: Repository 구현체 분리
 * Entity-Domain 통합으로 변환 로직 없음
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional
public class UserCouponRepositoryJpaImpl implements UserCouponRepository {

    private final UserCouponJpaRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserCoupon> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserCoupon> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId) {
        return jpaRepository.findByUserIdAndCouponId(userId, couponId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserCoupon> findAvailableCouponsByUserId(Long userId) {
        return jpaRepository.findAvailableCouponsByUserId(userId);
    }

    @Override
    @Transactional
    public UserCoupon save(UserCoupon userCoupon) {
        log.debug("💾 사용자 쿠폰 저장: userId = {}, couponId = {}, status = {}",
                userCoupon.getUserId(), userCoupon.getCouponId(), userCoupon.getStatus());

        // ✅ 변환 로직 없이 직접 저장
        return jpaRepository.save(userCoupon);
    }

    @Override
    @Transactional
    public void delete(UserCoupon userCoupon) {
        jpaRepository.delete(userCoupon);
        log.debug("🗑️ 사용자 쿠폰 삭제: id = {}", userCoupon.getId());
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
        log.debug("🗑️ 사용자 쿠폰 삭제: id = {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserCoupon> findAll() {
        return jpaRepository.findAll();
    }
}