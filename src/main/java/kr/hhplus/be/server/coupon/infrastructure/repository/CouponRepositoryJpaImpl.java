package kr.hhplus.be.server.coupon.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.dto.IssuedCouponResponse;
import kr.hhplus.be.server.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository 구현체 분리
 * Entity-Domain 통합으로 변환 로직 없음
 * N+1 문제 해결을 위한 배치 조회 메서드 추가
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional
public class CouponRepositoryJpaImpl implements CouponRepository {

    private final CouponJpaRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Coupon> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Coupon> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Coupon> findAllById(Iterable<Long> ids) {
        return jpaRepository.findAllById(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Coupon> findAvailableCoupons() {
        return jpaRepository.findAvailableCoupons(LocalDateTime.now());
    }

    @Override
    @Transactional
    public Coupon save(Coupon coupon) {
        log.debug("쿠폰 저장: name = {}, type = {}, quantity = {}/{}",
                coupon.getName(), coupon.getDiscountType(),
                coupon.getIssuedQuantity(), coupon.getTotalQuantity());

        return jpaRepository.save(coupon);
    }

    @Override
    @Transactional
    public void delete(Coupon coupon) {
        jpaRepository.delete(coupon);
        log.debug("쿠폰 삭제: id = {}", coupon.getId());
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
        log.debug("쿠폰 삭제: id = {}", id);
    }

    @Override
    @Transactional
    public IssuedCouponResponse issueWithTransaction(Long couponId, Long userId, 
            Supplier<IssuedCouponResponse> issueCouponLogic) {
        log.debug("인프라 레이어 트랜잭션 시작: couponId = {}, userId = {}", couponId, userId);
        return issueCouponLogic.get();
    }

    // 비관적 락 메서드 제거 - 분산락으로 대체
}