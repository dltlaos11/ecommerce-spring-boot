package kr.hhplus.be.server.coupon.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository 구현체 분리
 * Entity-Domain 통합으로 변환 로직 없음
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
    public List<Coupon> findAvailableCoupons() {
        return jpaRepository.findAvailableCoupons(LocalDateTime.now());
    }

    @Override
    public Coupon save(Coupon coupon) {
        log.debug("💾 쿠폰 저장: name = {}, type = {}, quantity = {}/{}",
                coupon.getName(), coupon.getDiscountType(),
                coupon.getIssuedQuantity(), coupon.getTotalQuantity());

        // 변환 로직 없이 직접 저장
        return jpaRepository.save(coupon);
    }

    @Override
    public void delete(Coupon coupon) {
        jpaRepository.delete(coupon);
        log.debug("🗑️ 쿠폰 삭제: id = {}", coupon.getId());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
        log.debug("🗑️ 쿠폰 삭제: id = {}", id);
    }

    @Override
    public Optional<Coupon> findByIdForUpdate(Long id) {
        log.debug("🔒 쿠폰 비관적 락 조회: id = {}", id);

        // SELECT FOR UPDATE로 선착순 쿠폰 발급 시 동시성 제어
        return jpaRepository.findByIdForUpdate(id);
    }
}
