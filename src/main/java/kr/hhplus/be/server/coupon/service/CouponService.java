package kr.hhplus.be.server.coupon.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
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
import kr.hhplus.be.server.coupon.infrastructure.repository.CouponJpaRepository;
import kr.hhplus.be.server.coupon.repository.CouponRepository;
import kr.hhplus.be.server.coupon.repository.UserCouponRepository;
import lombok.extern.slf4j.Slf4j;

// 동시성 제어: 비관적 락 + 유니크 제약 이중 방어
@Slf4j
@Service
@Transactional(readOnly = true)
public class CouponService {

        private final CouponRepository couponRepository;
        private final CouponJpaRepository couponJpaRepository; // 직접 접근용
        private final UserCouponRepository userCouponRepository;

        public CouponService(CouponRepository couponRepository,
                        CouponJpaRepository couponJpaRepository,
                        UserCouponRepository userCouponRepository) {
                this.couponRepository = couponRepository;
                this.couponJpaRepository = couponJpaRepository;
                this.userCouponRepository = userCouponRepository;
        }

        public List<AvailableCouponResponse> getAvailableCoupons() {
                List<Coupon> availableCoupons = couponRepository.findAvailableCoupons();
                return availableCoupons.stream()
                                .map(this::convertToAvailableResponse)
                                .toList();
        }

        // 분산락 기반 쿠폰 발급 (비관적 락 제거)
        @Transactional
        public IssuedCouponResponse issueCoupon(Long couponId, Long userId) {
                try {
                        Coupon coupon = couponRepository.findById(couponId)
                                        .orElseThrow(() -> new CouponNotFoundException(ErrorCode.COUPON_NOT_FOUND));

                        boolean alreadyIssued = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                                        .isPresent();

                        if (alreadyIssued) {
                                throw new CouponAlreadyIssuedException(ErrorCode.COUPON_ALREADY_ISSUED);
                        }

                        coupon.validateIssuable();
                        coupon.issue();
                        Coupon savedCoupon = couponRepository.save(coupon);

                        UserCoupon userCoupon = new UserCoupon(userId, couponId);
                        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

                        return convertToIssuedResponse(savedUserCoupon, savedCoupon);

                } catch (DataIntegrityViolationException e) {
                        throw new CouponAlreadyIssuedException(ErrorCode.COUPON_ALREADY_ISSUED);
                }
        }

        /**
         * 선착순 쿠폰 발급 (분산락 기반)
         */
        public IssuedCouponResponse issueFirstComeCoupon(Long couponId, Long userId) {
                try {
                        Coupon coupon = couponRepository.findById(couponId)
                                        .orElseThrow(() -> new CouponNotFoundException(ErrorCode.COUPON_NOT_FOUND));

                        if (coupon.isExhausted()) {
                                log.warn("선착순 쿠폰 소진: couponId = {}, 발급량 = {}/{}",
                                                couponId, coupon.getIssuedQuantity(), coupon.getTotalQuantity());
                                throw new kr.hhplus.be.server.coupon.exception.CouponExhaustedException(
                                                ErrorCode.COUPON_EXHAUSTED);
                        }

                        if (coupon.isExpired()) {
                                log.warn("선착순 쿠폰 만료: couponId = {}", couponId);
                                throw new kr.hhplus.be.server.coupon.exception.CouponExpiredException(
                                                ErrorCode.COUPON_EXPIRED);
                        }

                        if (userCouponRepository.findByUserIdAndCouponId(userId, couponId).isPresent()) {
                                log.warn("선착순 쿠폰 중복 발급: userId = {}, couponId = {}", userId, couponId);
                                throw new CouponAlreadyIssuedException(ErrorCode.COUPON_ALREADY_ISSUED);
                        }

                        coupon.issue();
                        Coupon savedCoupon = couponRepository.save(coupon);

                        UserCoupon userCoupon = new UserCoupon(userId, couponId);
                        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

                        return convertToIssuedResponse(savedUserCoupon, savedCoupon);

                } catch (DataIntegrityViolationException e) {
                        log.warn("DB 제약 위반 - 중복 발급: userId = {}, couponId = {}", userId, couponId);
                        throw new CouponAlreadyIssuedException(ErrorCode.COUPON_ALREADY_ISSUED);
                }
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
         */
        public CouponValidationResponse validateAndCalculateDiscount(Long userId, Long couponId,
                        BigDecimal orderAmount) {
                try {
                        UserCoupon userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                                        .orElseThrow(() -> new IllegalArgumentException("보유하지 않은 쿠폰입니다."));

                        Coupon coupon = couponRepository.findById(couponId)
                                        .orElseThrow(() -> new CouponNotFoundException(ErrorCode.COUPON_NOT_FOUND));

                        if (!userCoupon.isUsable()) {
                                return new CouponValidationResponse(
                                                couponId, userId, false, BigDecimal.ZERO, orderAmount,
                                                "사용할 수 없는 쿠폰입니다. (상태: " + userCoupon.getStatus().getDescription()
                                                                + ")");
                        }

                        BigDecimal discountAmount = coupon.calculateDiscountAmount(orderAmount);
                        BigDecimal finalAmount = orderAmount.subtract(discountAmount);

                        return new CouponValidationResponse(
                                        couponId, userId, true, discountAmount, finalAmount, null);

                } catch (Exception e) {
                        log.warn("쿠폰 검증 실패: userId = {}, couponId = {}, reason = {}",
                                        userId, couponId, e.getMessage());

                        return new CouponValidationResponse(
                                        couponId, userId, false, BigDecimal.ZERO, orderAmount, e.getMessage());
                }
        }

        /**
         * 쿠폰 사용 처리 - 동시성 안전
         */
        @Transactional
        public BigDecimal useCoupon(Long userId, Long couponId, BigDecimal orderAmount) {
                UserCoupon userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                                .orElseThrow(() -> new IllegalArgumentException("보유하지 않은 쿠폰입니다."));

                Coupon coupon = couponRepository.findById(couponId)
                                .orElseThrow(() -> new CouponNotFoundException(ErrorCode.COUPON_NOT_FOUND));

                BigDecimal discountAmount = coupon.calculateDiscountAmount(orderAmount);

                userCoupon.use();
                userCouponRepository.save(userCoupon);

                return discountAmount;
        }

        /**
         * 특정 쿠폰 조회
         */
        public AvailableCouponResponse getCoupon(Long couponId) {
                Coupon coupon = couponRepository.findById(couponId)
                                .orElseThrow(() -> new CouponNotFoundException(ErrorCode.COUPON_NOT_FOUND));

                return convertToAvailableResponse(coupon);
        }

        /**
         * 쿠폰 생성 (관리자 기능)
         */
        @Transactional
        public AvailableCouponResponse createCoupon(String name, Coupon.DiscountType discountType,
                        BigDecimal discountValue, Integer totalQuantity,
                        BigDecimal maxDiscountAmount, BigDecimal minimumOrderAmount,
                        java.time.LocalDateTime expiredAt) {
                Coupon coupon = new Coupon(name, discountType, discountValue, totalQuantity,
                                maxDiscountAmount, minimumOrderAmount, expiredAt);
                Coupon savedCoupon = couponRepository.save(coupon);

                return convertToAvailableResponse(savedCoupon);
        }

        /**
         * 쿠폰 발급 통계 조회 (관리자용)
         */
        public Map<String, Object> getCouponStatistics(Long couponId) {
                Coupon coupon = couponRepository.findById(couponId)
                                .orElseThrow(() -> new CouponNotFoundException(ErrorCode.COUPON_NOT_FOUND));

                return Map.of(
                                "couponId", coupon.getId(),
                                "couponName", coupon.getName(),
                                "totalQuantity", coupon.getTotalQuantity(),
                                "issuedQuantity", coupon.getIssuedQuantity(),
                                "remainingQuantity", coupon.getRemainingQuantity(),
                                "issuanceRate", (double) coupon.getIssuedQuantity() / coupon.getTotalQuantity() * 100,
                                "isExhausted", coupon.isExhausted(),
                                "isExpired", coupon.isExpired());
        }

        private AvailableCouponResponse convertToAvailableResponse(Coupon coupon) {
                return new AvailableCouponResponse(
                                coupon.getId(),
                                coupon.getName(),
                                coupon.getDiscountType().name(),
                                coupon.getDiscountValue(),
                                coupon.getMaxDiscountAmount(),
                                coupon.getMinimumOrderAmount(),
                                coupon.getRemainingQuantity(),
                                coupon.getTotalQuantity(),
                                coupon.getExpiredAt());
        }

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