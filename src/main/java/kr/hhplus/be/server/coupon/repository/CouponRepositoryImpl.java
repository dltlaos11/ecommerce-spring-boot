package kr.hhplus.be.server.coupon.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import kr.hhplus.be.server.coupon.domain.Coupon;

/**
 * 인메모리 쿠폰 저장소 구현체 (STEP05 기본 버전)
 * 
 * 기술적 특징:
 * - ConcurrentHashMap: 스레드 안전한 해시맵
 * - AtomicLong: 원자적 ID 생성
 * - 비관적 락 준비 (STEP06에서 활용)
 * 
 * STEP06에서 추가될 것:
 * - 선착순 쿠폰 발급을 위한 락 메커니즘
 * - 분산 락 지원
 */
@Repository
public class CouponRepositoryImpl implements CouponRepository {

    // 💾 인메모리 데이터 저장소
    private final Map<Long, Coupon> coupons = new ConcurrentHashMap<>();

    // 🔢 ID 자동 생성기
    private final AtomicLong idGenerator = new AtomicLong(1);

    // 🔒 쿠폰별 락 관리 (비관적 락용 - STEP06에서 활용)
    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public Optional<Coupon> findById(Long id) {
        return Optional.ofNullable(coupons.get(id));
    }

    @Override
    public List<Coupon> findAll() {
        return new ArrayList<>(coupons.values());
    }

    @Override
    public List<Coupon> findAvailableCoupons() {
        return coupons.values().stream()
                .filter(Coupon::isAvailable)
                .collect(Collectors.toList());
    }

    @Override
    public Coupon save(Coupon coupon) {
        if (coupon.getId() == null) {
            // 새 쿠폰: ID 자동 생성
            Long newId = idGenerator.getAndIncrement();
            coupon.setId(newId);

            // 락도 함께 생성 (STEP06에서 활용)
            locks.put(newId, new ReentrantLock());
        }

        // 저장 시간 갱신
        coupon.setUpdatedAt(LocalDateTime.now());

        // 저장
        coupons.put(coupon.getId(), coupon);

        return coupon;
    }

    @Override
    public void delete(Coupon coupon) {
        if (coupon.getId() != null) {
            coupons.remove(coupon.getId());
            locks.remove(coupon.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        coupons.remove(id);
        locks.remove(id);
    }

    @Override
    public Optional<Coupon> findByIdForUpdate(Long id) {
        /*
         * 🔒 비관적 락 구현 (STEP06에서 활용)
         * 
         * 실제 동작:
         * 1. 해당 쿠폰의 락 객체 획득
         * 2. 락 보유 중인 동안은 다른 스레드 대기
         * 3. 비즈니스 로직 완료 후 트랜잭션 종료 시 락 해제
         */
        ReentrantLock lock = locks.get(id);
        if (lock != null) {
            lock.lock(); // 🔒 락 획득 (STEP06에서 활용)
        }

        return findById(id);
    }

    /**
     * 애플리케이션 시작 시 테스트 데이터 자동 생성
     */
    @PostConstruct
    public void initData() {
        System.out.println("🎫 쿠폰 테스트 데이터 초기화 시작...");

        // 1. 정액 할인 쿠폰 (5,000원 할인)
        save(new Coupon(
                "신규 가입 쿠폰",
                Coupon.DiscountType.FIXED,
                new BigDecimal("5000"),
                100,
                new BigDecimal("5000"),
                new BigDecimal("30000"),
                LocalDateTime.now().plusDays(30)));

        // 2. 정률 할인 쿠폰 (10% 할인, 최대 50,000원)
        save(new Coupon(
                "VIP 10% 할인 쿠폰",
                Coupon.DiscountType.PERCENTAGE,
                new BigDecimal("10"),
                50,
                new BigDecimal("50000"),
                new BigDecimal("100000"),
                LocalDateTime.now().plusDays(7)));

        // 3. 선착순 쿠폰 (20% 할인, 제한된 수량)
        save(new Coupon(
                "선착순 20% 할인",
                Coupon.DiscountType.PERCENTAGE,
                new BigDecimal("20"),
                10, // 적은 수량으로 선착순 테스트
                new BigDecimal("100000"),
                new BigDecimal("50000"),
                LocalDateTime.now().plusDays(3)));

        // 4. 만료된 쿠폰 (테스트용)
        save(new Coupon(
                "만료된 쿠폰",
                Coupon.DiscountType.FIXED,
                new BigDecimal("10000"),
                50,
                new BigDecimal("10000"),
                new BigDecimal("20000"),
                LocalDateTime.now().minusDays(1) // 이미 만료됨
        ));

        System.out.println("✅ 쿠폰 테스트 데이터 초기화 완료! 총 " + coupons.size() + "개 쿠폰");
    }

    /**
     * 테스트 및 개발용: 모든 데이터 초기화
     */
    public void clear() {
        coupons.clear();
        locks.clear();
        idGenerator.set(1);
        System.out.println("🗑️ 쿠폰 데이터 모두 삭제됨");
    }

    /**
     * 현재 저장된 쿠폰 수 반환 (디버깅용)
     */
    public int count() {
        return coupons.size();
    }
}
