package kr.hhplus.be.server.coupon.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import kr.hhplus.be.server.coupon.domain.UserCoupon;

/**
 * 인메모리 사용자 쿠폰 저장소 구현체 (STEP05 기본 버전)
 */
@Repository
public class UserCouponRepositoryImpl implements UserCouponRepository {

    // 💾 인메모리 데이터 저장소
    private final Map<Long, UserCoupon> userCoupons = new ConcurrentHashMap<>();

    // 🔢 ID 자동 생성기
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<UserCoupon> findById(Long id) {
        return Optional.ofNullable(userCoupons.get(id));
    }

    @Override
    public List<UserCoupon> findByUserId(Long userId) {
        return userCoupons.values().stream()
                .filter(uc -> uc.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId) {
        return userCoupons.values().stream()
                .filter(uc -> uc.getUserId().equals(userId) && uc.getCouponId().equals(couponId))
                .findFirst();
    }

    @Override
    public List<UserCoupon> findAvailableCouponsByUserId(Long userId) {
        return userCoupons.values().stream()
                .filter(uc -> uc.getUserId().equals(userId) && uc.isUsable())
                .collect(Collectors.toList());
    }

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        if (userCoupon.getId() == null) {
            // 새 사용자 쿠폰: ID 자동 생성
            Long newId = idGenerator.getAndIncrement();
            userCoupon.setId(newId);
        }

        // 저장 시간 갱신
        userCoupon.setUpdatedAt(LocalDateTime.now());

        // 저장
        userCoupons.put(userCoupon.getId(), userCoupon);

        return userCoupon;
    }

    @Override
    public void delete(UserCoupon userCoupon) {
        if (userCoupon.getId() != null) {
            userCoupons.remove(userCoupon.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        userCoupons.remove(id);
    }

    @Override
    public List<UserCoupon> findAll() {
        return new ArrayList<>(userCoupons.values());
    }

    /**
     * 테스트 및 개발용: 모든 데이터 초기화
     */
    public void clear() {
        userCoupons.clear();
        idGenerator.set(1);
        System.out.println("🗑️ 사용자 쿠폰 데이터 모두 삭제됨");
    }

    /**
     * 현재 저장된 사용자 쿠폰 수 반환 (디버깅용)
     */
    public int count() {
        return userCoupons.size();
    }
}