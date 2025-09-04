package kr.hhplus.be.server.coupon.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.hhplus.be.server.common.event.EventPublisher;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.dto.AsyncCouponIssueRequest;
import kr.hhplus.be.server.coupon.dto.AsyncCouponIssueResponse;
import kr.hhplus.be.server.coupon.event.CouponIssueEvent;
import kr.hhplus.be.server.coupon.event.CouponIssueRequestEvent;
import kr.hhplus.be.server.coupon.exception.CouponAlreadyIssuedException;
import kr.hhplus.be.server.coupon.exception.CouponExhaustedException;
import kr.hhplus.be.server.coupon.exception.CouponNotFoundException;
import kr.hhplus.be.server.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 기반 비동기 쿠폰 발급 서비스
 * 
 * 핵심 구현 방식:
 * 1. Redis Set을 활용한 중복 발급 방지 (O(1) 검증)
 * 2. Redis String을 활용한 재고 관리 (원자성 보장)
 * 3. Redis Sorted Set을 활용한 비동기 처리 큐
 * 4. UUID 기반 요청 상태 추적
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCouponService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CouponRepository couponRepository;
    private final ObjectMapper objectMapper;
    private final EventPublisher eventPublisher;

    // Redis 키 패턴
    private static final String COUPON_ISSUED_KEY = "coupon:issued:"; // + couponId
    private static final String COUPON_STOCK_KEY = "coupon:stock:"; // + couponId
    private static final String COUPON_QUEUE_KEY = "coupon:queue:processing";
    private static final String REQUEST_STATUS_KEY = "coupon:request:"; // + requestId

    // Redis 타입 변환 유틸리티
    private String toRedisString(Long value) {
        return value != null ? value.toString() : null;
    }

    private String toRedisString(Integer value) {
        return value != null ? value.toString() : null;
    }

    /**
     * 선착순 쿠폰 발급 요청 (비동기)
     * 
     * 1단계: Redis 필터링 (빠른 실패)
     * 2단계: 비동기 큐에 요청 추가
     */
    public AsyncCouponIssueResponse requestCouponIssueAsync(AsyncCouponIssueRequest request) {
        String requestId = UUID.randomUUID().toString();
        Long userId = request.userId();
        Long couponId = request.couponId();
        LocalDateTime requestedAt = LocalDateTime.now();

        log.info("🎫 비동기 쿠폰 발급 요청: requestId={}, userId={}, couponId={}",
                requestId, userId, couponId);

        try {
            // 1단계: Redis 기반 빠른 검증
            validateFromRedis(userId, couponId);

            // 2단계: 재고 확인 및 차감 (원자적 연산)
            if (!decrementStock(couponId)) {
                log.warn("❌ 쿠폰 재고 소진: couponId={}", couponId);
                throw new CouponExhaustedException(ErrorCode.COUPON_EXHAUSTED);
            }

            // 3단계: 중복 발급 방지를 위해 Redis Set에 추가
            markAsIssued(userId, couponId);

            // 4단계: Kafka로 쿠폰 발급 이벤트 발행 (즉시 발행)
            CouponIssueEvent couponIssueEvent = CouponIssueEvent.create(couponId, userId, requestId);
            eventPublisher.publishEvent(couponIssueEvent);

            log.info("📤 Kafka로 쿠폰 발급 이벤트 발행: requestId={}, eventId={}",
                    requestId, couponIssueEvent.eventId());

            // 5단계: 요청 상태 저장
            AsyncCouponIssueResponse response = AsyncCouponIssueResponse.pending(requestId, requestedAt);
            saveRequestStatus(requestId, response);

            log.info("✅ 쿠폰 발급 요청 접수: requestId={}", requestId);
            return response;

        } catch (Exception e) {
            log.error("❌ 쿠폰 발급 요청 실패: requestId={}, error={}", requestId, e.getMessage());

            // 실패 시 롤백
            rollbackRedisState(userId, couponId);

            AsyncCouponIssueResponse failedResponse = AsyncCouponIssueResponse.failed(
                    requestId, requestedAt, LocalDateTime.now(), e.getMessage());
            saveRequestStatus(requestId, failedResponse);

            throw e;
        }
    }

    /**
     * 요청 상태 조회 (폴링 API)
     */
    public AsyncCouponIssueResponse getRequestStatus(String requestId) {
        try {
            String statusJson = (String) redisTemplate.opsForValue().get(REQUEST_STATUS_KEY + requestId);

            if (statusJson == null) {
                throw new IllegalArgumentException("존재하지 않는 요청 ID입니다: " + requestId);
            }

            return objectMapper.readValue(statusJson, AsyncCouponIssueResponse.class);

        } catch (JsonProcessingException e) {
            log.error("요청 상태 역직렬화 실패: requestId={}", requestId, e);
            throw new RuntimeException("요청 상태 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 쿠폰 재고를 Redis에 초기화
     * (관리자 기능 또는 애플리케이션 시작 시 호출)
     */
    public void initializeCouponStock(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponNotFoundException(ErrorCode.COUPON_NOT_FOUND));

        String stockKey = COUPON_STOCK_KEY + couponId;
        Integer remainingStock = coupon.getRemainingQuantity();

        redisTemplate.opsForValue().set(stockKey, toRedisString(remainingStock), Duration.ofDays(30));

        log.info("🔄 쿠폰 재고 Redis 초기화: couponId={}, stock={}", couponId, remainingStock);
    }

    /**
     * Redis 기반 빠른 검증
     */
    private void validateFromRedis(Long userId, Long couponId) {
        // 중복 발급 체크 (O(1) 연산)
        String issuedKey = COUPON_ISSUED_KEY + couponId;
        if (redisTemplate.opsForSet().isMember(issuedKey, toRedisString(userId))) {
            log.warn("❌ 중복 발급 방지: userId={}, couponId={}", userId, couponId);
            throw new CouponAlreadyIssuedException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        // 재고 확인
        String stockKey = COUPON_STOCK_KEY + couponId;
        String stock = (String) redisTemplate.opsForValue().get(stockKey);

        if (stock == null) {
            // Redis에 재고 정보가 없으면 DB에서 초기화
            initializeCouponStock(couponId);
            stock = (String) redisTemplate.opsForValue().get(stockKey);
        }

        if (stock == null || Integer.parseInt(stock) <= 0) {
            log.warn("❌ 재고 부족: couponId={}, stock={}", couponId, stock);
            throw new CouponExhaustedException(ErrorCode.COUPON_EXHAUSTED);
        }
    }

    /**
     * 원자적 재고 차감
     */
    private boolean decrementStock(Long couponId) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        Long remaining = redisTemplate.opsForValue().decrement(stockKey);

        if (remaining != null && remaining >= 0) {
            return true;
        } else {
            // 재고가 0 이하가 되면 다시 증가시켜 롤백
            redisTemplate.opsForValue().increment(stockKey);
            return false;
        }
    }

    /**
     * 중복 발급 방지를 위해 Redis Set에 사용자 추가
     */
    private void markAsIssued(Long userId, Long couponId) {
        String issuedKey = COUPON_ISSUED_KEY + couponId;
        redisTemplate.opsForSet().add(issuedKey, toRedisString(userId));
        redisTemplate.expire(issuedKey, Duration.ofDays(30)); // TTL 설정
    }

    /**
     * 비동기 처리 큐에 요청 추가
     */
    private void addToProcessingQueue(CouponIssueRequestEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            double score = System.currentTimeMillis(); // 타임스탬프 기반 순서 보장

            redisTemplate.opsForZSet().add(COUPON_QUEUE_KEY, eventJson, score);

            log.debug("📤 처리 큐 추가: requestId={}, score={}", event.getRequestId(), score);

        } catch (JsonProcessingException e) {
            log.error("이벤트 직렬화 실패: {}", event, e);
            throw new RuntimeException("요청 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 요청 상태 저장
     */
    private void saveRequestStatus(String requestId, AsyncCouponIssueResponse response) {
        try {
            String statusJson = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(
                    REQUEST_STATUS_KEY + requestId,
                    statusJson,
                    Duration.ofHours(24) // 24시간 TTL
            );

        } catch (JsonProcessingException e) {
            log.error("상태 저장 실패: requestId={}", requestId, e);
        }
    }

    /**
     * 실패 시 Redis 상태 롤백
     */
    private void rollbackRedisState(Long userId, Long couponId) {
        try {
            // 재고 복구
            String stockKey = COUPON_STOCK_KEY + couponId;
            redisTemplate.opsForValue().increment(stockKey);

            // 발급 표시 제거
            String issuedKey = COUPON_ISSUED_KEY + couponId;
            redisTemplate.opsForSet().remove(issuedKey, toRedisString(userId));

            log.debug("🔄 Redis 상태 롤백: userId={}, couponId={}", userId, couponId);

        } catch (Exception e) {
            log.error("Redis 롤백 실패: userId={}, couponId={}", userId, couponId, e);
        }
    }

    /**
     * 처리 큐 크기 조회 (모니터링용)
     */
    public Long getQueueSize() {
        return redisTemplate.opsForZSet().zCard(COUPON_QUEUE_KEY);
    }

    /**
     * 쿠폰별 현재 재고 조회 (모니터링용)
     */
    public Integer getCurrentStock(Long couponId) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        String stock = (String) redisTemplate.opsForValue().get(stockKey);
        return stock != null ? Integer.parseInt(stock) : null;
    }
}