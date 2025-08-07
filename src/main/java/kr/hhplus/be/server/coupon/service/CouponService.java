package kr.hhplus.be.server.coupon.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.coupon.dto.AvailableCouponResponse;
import kr.hhplus.be.server.coupon.dto.CouponValidationResponse;
import kr.hhplus.be.server.coupon.dto.IssuedCouponResponse;
import kr.hhplus.be.server.coupon.dto.UserCouponResponse;
import kr.hhplus.be.server.coupon.exception.CouponAlreadyIssuedException;
import kr.hhplus.be.server.coupon.exception.CouponNotFoundException;
import kr.hhplus.be.server.coupon.repository.CouponRepository;
import kr.hhplus.be.server.coupon.repository.UserCouponRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * 쿠폰 서비스
 * 
 * 설계 원칙:
 * - 단일 책임: 쿠폰 관련 비즈니스 로직만 처리
 * - 의존성 역전: Repository 인터페이스에만 의존
 * - 트랜잭션 관리로 데이터 일관성 보장
 * - STEP06에서 선착순 처리 및 동시성 제어 추가 예정
 * 
 * 책임:
 * - 쿠폰 발급/조회/검증
 * - 할인 금액 계산
 * - 중복 발급 방지
 * - DTO 변환
 */
@Slf4j
@Service
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션
public class CouponService {

        private final CouponRepository couponRepository;
        private final UserCouponRepository userCouponRepository;

        /**
         * 생성자 주입 (스프링 권장 방식)
         */
        public CouponService(CouponRepository couponRepository,
                        UserCouponRepository userCouponRepository) {
                this.couponRepository = couponRepository;
                this.userCouponRepository = userCouponRepository;
        }

        /**
         * 발급 가능한 쿠폰 목록 조회
         * 
         * @return 현재 발급 가능한 쿠폰 목록
         */
        public List<AvailableCouponResponse> getAvailableCoupons() {
                log.debug("🎫 발급 가능한 쿠폰 목록 조회 요청");

                List<Coupon> availableCoupons = couponRepository.findAvailableCoupons();

                log.debug("✅ 발급 가능한 쿠폰 조회 완료: {}개", availableCoupons.size());

                return availableCoupons.stream()
                                .map(this::convertToAvailableResponse)
                                .toList();
        }

        /**
         * 쿠폰 발급
         */
        @Transactional
        public IssuedCouponResponse issueCoupon(Long couponId, Long userId) {
                log.info("쿠폰 발급: couponId = {}, userId = {}", couponId, userId);

                Coupon coupon = couponRepository.findById(couponId)
                                .orElseThrow(() -> {
                                        log.error("쿠폰 발급 실패 - 쿠폰 없음: couponId = {}", couponId);
                                        return new CouponNotFoundException(ErrorCode.COUPON_NOT_FOUND);
                                });

                userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                                .ifPresent(existingCoupon -> {
                                        log.warn("쿠폰 발급 실패 - 중복 발급: userId = {}, couponId = {}", userId, couponId);
                                        throw new CouponAlreadyIssuedException(ErrorCode.COUPON_ALREADY_ISSUED);
                                });

                coupon.validateIssuable();

                coupon.issue();
                Coupon savedCoupon = couponRepository.save(coupon);

                UserCoupon userCoupon = new UserCoupon(userId, couponId);
                UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

                return convertToIssuedResponse(savedUserCoupon, savedCoupon);
        }

        /**
         * 사용자 보유 쿠폰 목록 조회 - N+1 문제 해결
         */
        public List<UserCouponResponse> getUserCoupons(Long userId) {
                List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);

                // N+1 문제 해결: 필요한 쿠폰 ID들을 한 번에 조회
                Set<Long> couponIds = userCoupons.stream()
                                .map(UserCoupon::getCouponId)
                                .collect(Collectors.toSet());

                Map<Long, Coupon> couponMap = couponRepository.findAllById(couponIds).stream()
                                .collect(Collectors.toMap(Coupon::getId, coupon -> coupon));

                return userCoupons.stream()
                                .map(userCoupon -> {
                                        Coupon coupon = couponMap.get(userCoupon.getCouponId());
                                        return convertToUserCouponResponse(userCoupon, coupon);
                                })
                                .toList();
        }

        /**
         * 사용자의 사용 가능한 쿠폰 목록 조회 - N+1 문제 해결
         */
        public List<UserCouponResponse> getAvailableUserCoupons(Long userId) {
                List<UserCoupon> availableUserCoupons = userCouponRepository.findAvailableCouponsByUserId(userId);

                // N+1 문제 해결: 필요한 쿠폰 ID들을 한 번에 조회
                Set<Long> couponIds = availableUserCoupons.stream()
                                .map(UserCoupon::getCouponId)
                                .collect(Collectors.toSet());

                Map<Long, Coupon> couponMap = couponRepository.findAllById(couponIds).stream()
                                .collect(Collectors.toMap(Coupon::getId, coupon -> coupon));

                return availableUserCoupons.stream()
                                .map(userCoupon -> {
                                        Coupon coupon = couponMap.get(userCoupon.getCouponId());
                                        return convertToUserCouponResponse(userCoupon, coupon);
                                })
                                .toList();
        }

        /**
         * 쿠폰 사용 가능 여부 검증 및 할인 금액 계산
         * 
         * @param userId      사용자 ID
         * @param couponId    쿠폰 ID
         * @param orderAmount 주문 금액
         * @return 쿠폰 검증 결과 및 할인 정보
         */
        public CouponValidationResponse validateAndCalculateDiscount(Long userId, Long couponId,
                        BigDecimal orderAmount) {
                log.debug("🧮 쿠폰 검증 및 할인 계산 요청: userId = {}, couponId = {}, orderAmount = {}",
                                userId, couponId, orderAmount);

                try {
                        // 1. 사용자 쿠폰 조회
                        UserCoupon userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                                        .orElseThrow(() -> new IllegalArgumentException("보유하지 않은 쿠폰입니다."));

                        // 2. 쿠폰 정보 조회
                        Coupon coupon = couponRepository.findById(couponId)
                                        .orElseThrow(() -> new CouponNotFoundException(ErrorCode.COUPON_NOT_FOUND));

                        // 3. 사용자 쿠폰 상태 확인
                        if (!userCoupon.isUsable()) {
                                return new CouponValidationResponse(
                                                couponId, userId, false, BigDecimal.ZERO, orderAmount,
                                                "사용할 수 없는 쿠폰입니다. (상태: " + userCoupon.getStatus().getDescription()
                                                                + ")");
                        }

                        // 4. 쿠폰 사용 조건 검증 및 할인 금액 계산
                        BigDecimal discountAmount = coupon.calculateDiscountAmount(orderAmount);
                        BigDecimal finalAmount = orderAmount.subtract(discountAmount);

                        log.debug("✅ 쿠폰 검증 성공: 할인금액 = {}, 최종금액 = {}", discountAmount, finalAmount);

                        return new CouponValidationResponse(
                                        couponId, userId, true, discountAmount, finalAmount, null);

                } catch (Exception e) {
                        log.warn("❌ 쿠폰 검증 실패: userId = {}, couponId = {}, reason = {}",
                                        userId, couponId, e.getMessage());

                        return new CouponValidationResponse(
                                        couponId, userId, false, BigDecimal.ZERO, orderAmount, e.getMessage());
                }
        }

        /**
         * 쿠폰 사용 처리 (주문 시 호출)
         */
        @Transactional
        public BigDecimal useCoupon(Long userId, Long couponId, BigDecimal orderAmount) {
                UserCoupon userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                                .orElseThrow(() -> {
                                        log.error("쿠폰 사용 실패 - 보유하지 않은 쿠폰: userId = {}, couponId = {}", userId,
                                                        couponId);
                                        return new IllegalArgumentException("보유하지 않은 쿠폰입니다.");
                                });

                Coupon coupon = couponRepository.findById(couponId)
                                .orElseThrow(() -> new CouponNotFoundException(ErrorCode.COUPON_NOT_FOUND));

                BigDecimal discountAmount = coupon.calculateDiscountAmount(orderAmount);

                userCoupon.use();
                userCouponRepository.save(userCoupon);

                return discountAmount;
        }

        /**
         * 특정 쿠폰 조회
         * 
         * @param couponId 쿠폰 ID
         * @return 쿠폰 정보
         */
        public AvailableCouponResponse getCoupon(Long couponId) {
                log.debug("🔍 쿠폰 조회 요청: couponId = {}", couponId);

                Coupon coupon = couponRepository.findById(couponId)
                                .orElseThrow(() -> {
                                        log.warn("❌ 쿠폰을 찾을 수 없음: couponId = {}", couponId);
                                        return new CouponNotFoundException(ErrorCode.COUPON_NOT_FOUND);
                                });

                log.debug("✅ 쿠폰 조회 완료: {}", coupon.getName());

                return convertToAvailableResponse(coupon);
        }

        /**
         * 쿠폰 생성 (관리자 기능)
         * 
         * @param name               쿠폰명
         * @param discountType       할인 타입
         * @param discountValue      할인 값
         * @param totalQuantity      총 수량
         * @param maxDiscountAmount  최대 할인 금액
         * @param minimumOrderAmount 최소 주문 금액
         * @param expiredAt          만료일
         * @return 생성된 쿠폰 정보
         */
        @Transactional
        public AvailableCouponResponse createCoupon(String name, Coupon.DiscountType discountType,
                        BigDecimal discountValue, Integer totalQuantity,
                        BigDecimal maxDiscountAmount, BigDecimal minimumOrderAmount,
                        java.time.LocalDateTime expiredAt) {
                log.info("🆕 쿠폰 생성 요청: '{}', 타입: {}, 수량: {}", name, discountType, totalQuantity);

                Coupon coupon = new Coupon(name, discountType, discountValue, totalQuantity,
                                maxDiscountAmount, minimumOrderAmount, expiredAt);
                Coupon savedCoupon = couponRepository.save(coupon);

                log.info("✅ 쿠폰 생성 완료: ID = {}, 이름 = '{}'", savedCoupon.getId(), savedCoupon.getName());

                return convertToAvailableResponse(savedCoupon);
        }

        /**
         * Coupon을 AvailableCouponResponse DTO로 변환
         */
        private AvailableCouponResponse convertToAvailableResponse(Coupon coupon) {
                return new AvailableCouponResponse(
                                coupon.getId(),
                                coupon.getName(),
                                coupon.getDiscountType().name(), // getCode() 대신 name() 사용
                                coupon.getDiscountValue(),
                                coupon.getMaxDiscountAmount(),
                                coupon.getMinimumOrderAmount(),
                                coupon.getRemainingQuantity(),
                                coupon.getTotalQuantity(),
                                coupon.getExpiredAt());
        }

        /**
         * UserCoupon과 Coupon을 IssuedCouponResponse DTO로 변환
         */
        private IssuedCouponResponse convertToIssuedResponse(UserCoupon userCoupon, Coupon coupon) {
                return new IssuedCouponResponse(
                                userCoupon.getId(),
                                coupon.getId(),
                                userCoupon.getUserId(),
                                coupon.getName(),
                                coupon.getDiscountType().name(),
                                coupon.getDiscountValue(),
                                coupon.getMaxDiscountAmount(),
                                coupon.getMinimumOrderAmount(),
                                coupon.getExpiredAt(),
                                userCoupon.getCreatedAt(),
                                userCoupon.getStatus().name());
        }

        /**
         * UserCoupon과 Coupon을 UserCouponResponse DTO로 변환
         */
        private UserCouponResponse convertToUserCouponResponse(UserCoupon userCoupon, Coupon coupon) {
                if (coupon == null) {
                        // 쿠폰이 삭제된 경우
                        return new UserCouponResponse(
                                        userCoupon.getId(),
                                        userCoupon.getCouponId(),
                                        "삭제된 쿠폰",
                                        "UNKNOWN",
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO,
                                        userCoupon.getStatus().name(),
                                        null,
                                        userCoupon.getCreatedAt(),
                                        userCoupon.getUsedAt());
                }

                return new UserCouponResponse(
                                userCoupon.getId(),
                                coupon.getId(),
                                coupon.getName(),
                                coupon.getDiscountType().name(),
                                coupon.getDiscountValue(),
                                coupon.getMaxDiscountAmount(),
                                coupon.getMinimumOrderAmount(),
                                userCoupon.getStatus().name(),
                                coupon.getExpiredAt(),
                                userCoupon.getCreatedAt(),
                                userCoupon.getUsedAt());
        }
}