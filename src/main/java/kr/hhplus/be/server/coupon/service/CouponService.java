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

/**
 * 쿠폰 서비스 - 동시성 제어 강화
 * 
 * 동시성 제어 전략:
 * - 쿠폰 발급: 비관적 락 + 유니크 제약 이중 방어
 * - 선착순 처리: SELECT FOR UPDATE
 * - DataIntegrityViolationException 처리
 */
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

        /**
         * 발급 가능한 쿠폰 목록 조회
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
         * 쿠폰 발급 - 비관적 락 + 유니크 제약 이중 방어
         * 🔒 선착순 쿠폰 발급의 핵심 동시성 제어
         */
        @Transactional
        public IssuedCouponResponse issueCoupon(Long couponId, Long userId) {
                log.info("🔒 동시성 제어 쿠폰 발급 시작: couponId = {}, userId = {}", couponId, userId);

                try {
                        // 1단계: 비관적 락으로 쿠폰 조회 (SELECT FOR UPDATE)
                        Coupon coupon = couponJpaRepository.findByIdForUpdate(couponId)
                                        .orElseThrow(() -> {
                                                log.error("쿠폰 발급 실패 - 쿠폰 없음: couponId = {}", couponId);
                                                return new CouponNotFoundException(ErrorCode.COUPON_NOT_FOUND);
                                        });

                        log.debug("🔒 쿠폰 비관적 락 획득: {}", coupon.getName());

                        // 2단계: 중복 발급 검증 (애플리케이션 레벨)
                        boolean alreadyIssued = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                                        .isPresent();

                        if (alreadyIssued) {
                                log.warn("쿠폰 발급 실패 - 중복 발급: userId = {}, couponId = {}", userId, couponId);
                                throw new CouponAlreadyIssuedException(ErrorCode.COUPON_ALREADY_ISSUED);
                        }

                        // 3단계: 쿠폰 발급 가능 여부 검증
                        coupon.validateIssuable();

                        // 4단계: 쿠폰 발급 처리 (도메인 로직)
                        coupon.issue();
                        Coupon savedCoupon = couponRepository.save(coupon);

                        // 5단계: 사용자 쿠폰 생성 (유니크 제약으로 중복 방지)
                        UserCoupon userCoupon = new UserCoupon(userId, couponId);
                        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

                        log.info("✅ 동시성 제어 쿠폰 발급 성공: userId = {}, couponId = {}, 남은수량 = {}",
                                        userId, couponId, savedCoupon.getRemainingQuantity());

                        return convertToIssuedResponse(savedUserCoupon, savedCoupon);

                } catch (DataIntegrityViolationException e) {
                        // 유니크 제약 위반 (DB 레벨 중복 방지)
                        log.warn("🔒 유니크 제약 위반 - 동시 발급 시도: userId = {}, couponId = {}", userId, couponId);
                        throw new CouponAlreadyIssuedException(ErrorCode.COUPON_ALREADY_ISSUED);
                }
        }

        /**
         * 선착순 쿠폰 발급 (더 엄격한 동시성 제어)
         */
        @Transactional
        public IssuedCouponResponse issueFirstComeCoupon(Long couponId, Long userId) {
                log.info("🏃‍♂️ 선착순 쿠폰 발급: couponId = {}, userId = {}", couponId, userId);

                try {
                        // 비관적 락으로 쿠폰 조회
                        Coupon coupon = couponJpaRepository.findByIdForUpdate(couponId)
                                        .orElseThrow(() -> new CouponNotFoundException(ErrorCode.COUPON_NOT_FOUND));

                        // 소진 여부 우선 확인 (빠른 실패)
                        if (coupon.isExhausted()) {
                                log.warn("선착순 쿠폰 소진: couponId = {}, 발급량 = {}/{}",
                                                couponId, coupon.getIssuedQuantity(), coupon.getTotalQuantity());
                                throw new kr.hhplus.be.server.coupon.exception.CouponExhaustedException(
                                                ErrorCode.COUPON_EXHAUSTED);
                        }

                        // 만료 여부 확인
                        if (coupon.isExpired()) {
                                log.warn("선착순 쿠폰 만료: couponId = {}", couponId);
                                throw new kr.hhplus.be.server.coupon.exception.CouponExpiredException(
                                                ErrorCode.COUPON_EXPIRED);
                        }

                        // 중복 발급 검증
                        if (userCouponRepository.findByUserIdAndCouponId(userId, couponId).isPresent()) {
                                log.warn("선착순 쿠폰 중복 발급: userId = {}, couponId = {}", userId, couponId);
                                throw new CouponAlreadyIssuedException(ErrorCode.COUPON_ALREADY_ISSUED);
                        }

                        // 발급 처리
                        coupon.issue();
                        Coupon savedCoupon = couponRepository.save(coupon);

                        // 사용자 쿠폰 생성
                        UserCoupon userCoupon = new UserCoupon(userId, couponId);
                        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

                        log.info("🎉 선착순 쿠폰 발급 성공: userId = {}, 순번 = {}/{}",
                                        userId, savedCoupon.getIssuedQuantity(), savedCoupon.getTotalQuantity());

                        return convertToIssuedResponse(savedUserCoupon, savedCoupon);

                } catch (DataIntegrityViolationException e) {
                        log.warn("🔒 DB 제약 위반 - 선착순 중복 발급: userId = {}, couponId = {}", userId, couponId);
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
         * 쿠폰 사용 처리 - 동시성 안전
         */
        @Transactional
        public BigDecimal useCoupon(Long userId, Long couponId, BigDecimal orderAmount) {
                log.info("🎫 쿠폰 사용 처리: userId = {}, couponId = {}, orderAmount = {}",
                                userId, couponId, orderAmount);

                // 사용자 쿠폰 조회
                UserCoupon userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                                .orElseThrow(() -> {
                                        log.error("쿠폰 사용 실패 - 보유하지 않은 쿠폰: userId = {}, couponId = {}", userId,
                                                        couponId);
                                        return new IllegalArgumentException("보유하지 않은 쿠폰입니다.");
                                });

                // 쿠폰 정보 조회
                Coupon coupon = couponRepository.findById(couponId)
                                .orElseThrow(() -> new CouponNotFoundException(ErrorCode.COUPON_NOT_FOUND));

                // 할인 금액 계산
                BigDecimal discountAmount = coupon.calculateDiscountAmount(orderAmount);

                // 쿠폰 사용 처리
                userCoupon.use();
                userCouponRepository.save(userCoupon);

                log.info("✅ 쿠폰 사용 완료: userId = {}, couponId = {}, 할인금액 = {}",
                                userId, couponId, discountAmount);

                return discountAmount;
        }

        /**
         * 특정 쿠폰 조회
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

        // ==================== DTO 변환 메서드들 ====================

        /**
         * Coupon을 AvailableCouponResponse DTO로 변환
         */
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