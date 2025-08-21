package kr.hhplus.be.server.coupon.worker;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.coupon.dto.AsyncCouponIssueResponse;
import kr.hhplus.be.server.coupon.event.CouponIssueRequestEvent;
import kr.hhplus.be.server.coupon.exception.CouponAlreadyIssuedException;
import kr.hhplus.be.server.coupon.exception.CouponNotFoundException;
import kr.hhplus.be.server.coupon.repository.CouponRepository;
import kr.hhplus.be.server.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 쿠폰 발급 비동기 워커
 * 
 * Redis Sorted Set 큐에서 요청을 하나씩 처리하는 단일 워커
 * 안전성을 위해 순차 처리 방식 채택
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueWorker {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final ObjectMapper objectMapper;

    private static final String COUPON_QUEUE_KEY = "coupon:queue:processing";
    private static final String REQUEST_STATUS_KEY = "coupon:request:";
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 스케줄러 기반 큐 처리
     * 1초마다 큐에서 하나씩 요청을 꺼내어 처리
     */
    @Scheduled(fixedDelay = 1000) // 1초마다 처리
    public void processCouponIssueQueue() {
        try {
            // ✅ 안전성: 하나씩 처리 (popMin으로 원자적 제거)
            ZSetOperations.TypedTuple<Object> request = redisTemplate.opsForZSet().popMin(COUPON_QUEUE_KEY);

            if (request != null && request.getValue() != null) {
                String requestJson = (String) request.getValue();
                Double score = request.getScore();

                log.debug("📋 큐에서 요청 처리 시작: score={}", score);

                try {
                    processRequest(requestJson);
                    log.debug("✅ 요청 처리 완료");

                } catch (Exception e) {
                    log.error("❌ 요청 처리 실패, 재시도 예약: error={}", e.getMessage());

                    // ✅ 실패 시 재시도를 위해 큐에 다시 추가 (1분 후)
                    double retryScore = System.currentTimeMillis() + 60000; // 1분 후 재시도
                    redisTemplate.opsForZSet().add(COUPON_QUEUE_KEY, requestJson, retryScore);
                }
            }

        } catch (Exception e) {
            log.error("큐 처리 중 예외 발생", e);
        }
    }

    /**
     * 개별 쿠폰 발급 요청 처리
     * 트랜잭션 내에서 실제 DB 작업 수행
     */
    @Transactional
    public void processRequest(String requestJson) throws JsonProcessingException {
        CouponIssueRequestEvent event = objectMapper.readValue(requestJson, CouponIssueRequestEvent.class);

        String requestId = event.getRequestId();
        Long userId = event.getUserId();
        Long couponId = event.getCouponId();
        LocalDateTime requestedAt = event.getRequestedAt();

        log.info("🔄 쿠폰 발급 처리 시작: requestId={}, userId={}, couponId={}",
                requestId, userId, couponId);

        // 처리 중 상태로 업데이트
        updateRequestStatus(requestId, AsyncCouponIssueResponse.processing(requestId, requestedAt));

        try {
            // 실제 쿠폰 발급 처리
            Long issuedCouponId = issueCouponToDatabase(userId, couponId);

            // 성공 상태로 업데이트
            AsyncCouponIssueResponse completedResponse = AsyncCouponIssueResponse.completed(
                    requestId, requestedAt, LocalDateTime.now(), issuedCouponId);
            updateRequestStatus(requestId, completedResponse);

            log.info("✅ 쿠폰 발급 처리 완료: requestId={}, issuedCouponId={}",
                    requestId, issuedCouponId);

        } catch (Exception e) {
            log.error("❌ 쿠폰 발급 처리 실패: requestId={}, error={}", requestId, e.getMessage());

            // 실패 상태로 업데이트
            AsyncCouponIssueResponse failedResponse = AsyncCouponIssueResponse.failed(
                    requestId, requestedAt, LocalDateTime.now(), e.getMessage());
            updateRequestStatus(requestId, failedResponse);

            throw e; // 재시도를 위해 예외 전파
        }
    }

    /**
     * 실제 데이터베이스에 쿠폰 발급
     * 기존 CouponService의 핵심 로직 재사용
     */
    private Long issueCouponToDatabase(Long userId, Long couponId) {
        try {
            // 최신 쿠폰 상태 조회 (DB 기준)
            Coupon coupon = couponRepository.findById(couponId)
                    .orElseThrow(() -> new CouponNotFoundException(ErrorCode.COUPON_NOT_FOUND));

            log.debug("DB 쿠폰 상태 확인: couponId={}, 발급량={}/{}",
                    couponId, coupon.getIssuedQuantity(), coupon.getTotalQuantity());

            // 최종 검증 (DB 기준으로 이중 체크)
            if (coupon.isExhausted()) {
                throw new kr.hhplus.be.server.coupon.exception.CouponExhaustedException(
                        ErrorCode.COUPON_EXHAUSTED);
            }

            if (coupon.isExpired()) {
                throw new kr.hhplus.be.server.coupon.exception.CouponExpiredException(
                        ErrorCode.COUPON_EXPIRED);
            }

            // 최종 중복 발급 검증 (DB 기준)
            boolean alreadyIssued = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                    .isPresent();

            if (alreadyIssued) {
                log.warn("DB 기준 중복 발급 감지: userId={}, couponId={}", userId, couponId);
                throw new CouponAlreadyIssuedException(ErrorCode.COUPON_ALREADY_ISSUED);
            }

            // 쿠폰 발급 수량 증가
            coupon.issueWithoutValidation();
            Coupon savedCoupon = couponRepository.save(coupon);

            // 사용자 쿠폰 생성
            UserCoupon userCoupon = new UserCoupon(userId, couponId);
            UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

            log.debug("DB 쿠폰 발급 완료: userCouponId={}, 발급량={}/{}",
                    savedUserCoupon.getId(), savedCoupon.getIssuedQuantity(), savedCoupon.getTotalQuantity());

            return savedUserCoupon.getId();

        } catch (DataIntegrityViolationException e) {
            log.warn("DB 제약 위반 - 중복 발급: userId={}, couponId={}", userId, couponId);
            throw new CouponAlreadyIssuedException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
    }

    /**
     * 요청 상태 업데이트
     */
    private void updateRequestStatus(String requestId, AsyncCouponIssueResponse response) {
        try {
            String statusJson = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(
                    REQUEST_STATUS_KEY + requestId,
                    statusJson,
                    Duration.ofHours(24) // 24시간 TTL
            );

        } catch (JsonProcessingException e) {
            log.error("상태 업데이트 실패: requestId={}", requestId, e);
        }
    }

    /**
     * 큐 상태 모니터링 (운영용)
     */
    @Scheduled(fixedRate = 30000) // 30초마다
    public void monitorQueueHealth() {
        try {
            Long queueSize = redisTemplate.opsForZSet().zCard(COUPON_QUEUE_KEY);

            if (queueSize != null && queueSize > 0) {
                log.info("📊 쿠폰 발급 큐 상태: 대기 중인 요청 수={}", queueSize);

                // 큐가 너무 길어지면 경고
                if (queueSize > 1000) {
                    log.warn("⚠️ 쿠폰 발급 큐 과부하: 대기 중인 요청 수={}", queueSize);
                }
            }

        } catch (Exception e) {
            log.error("큐 모니터링 중 오류", e);
        }
    }
}