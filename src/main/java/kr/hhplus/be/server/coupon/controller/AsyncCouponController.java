package kr.hhplus.be.server.coupon.controller;

import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.coupon.dto.AsyncCouponIssueRequest;
import kr.hhplus.be.server.coupon.dto.AsyncCouponIssueResponse;
import kr.hhplus.be.server.coupon.service.RedisCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * Redis 기반 비동기 쿠폰 발급 API 컨트롤러
 * 
 * STEP 14: 선착순 쿠폰 발급을 위한 비동기 처리 시스템
 */
@Slf4j
@RestController
@RequestMapping("/api/coupons/async")
@RequiredArgsConstructor
public class AsyncCouponController {
    
    private final RedisCouponService redisCouponService;
    
    /**
     * 비동기 쿠폰 발급 요청
     * 
     * 즉시 응답을 반환하고, 실제 발급은 백그라운드에서 처리
     * 
     * @param request 쿠폰 발급 요청 정보
     * @return 요청 접수 결과 (requestId 포함)
     */
    @PostMapping("/issue")
    public CommonResponse<AsyncCouponIssueResponse> issueCouponAsync(
            @Valid @RequestBody AsyncCouponIssueRequest request) {
        
        log.info("🎫 비동기 쿠폰 발급 API 호출: userId={}, couponId={}", 
                request.getUserId(), request.getCouponId());
        
        try {
            AsyncCouponIssueResponse response = redisCouponService.requestCouponIssueAsync(request);
            
            log.info("✅ 비동기 쿠폰 발급 요청 접수: requestId={}", response.getRequestId());
            
            return CommonResponse.success(response);
            
        } catch (Exception e) {
            log.error("❌ 비동기 쿠폰 발급 요청 실패: userId={}, couponId={}, error={}", 
                    request.getUserId(), request.getCouponId(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * 쿠폰 발급 요청 상태 조회 (폴링 API)
     * 
     * 클라이언트가 주기적으로 호출하여 처리 상태를 확인
     * 
     * @param requestId 요청 추적 ID
     * @return 현재 처리 상태
     */
    @GetMapping("/status/{requestId}")
    public CommonResponse<AsyncCouponIssueResponse> getIssueStatus(@PathVariable String requestId) {
        
        log.debug("📊 쿠폰 발급 상태 조회: requestId={}", requestId);
        
        try {
            AsyncCouponIssueResponse response = redisCouponService.getRequestStatus(requestId);
            
            log.debug("✅ 상태 조회 완료: requestId={}, status={}", requestId, response.getStatus());
            
            return CommonResponse.success(response);
            
        } catch (Exception e) {
            log.error("❌ 상태 조회 실패: requestId={}, error={}", requestId, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 쿠폰 재고 초기화 (관리자 API)
     * 
     * 쿠폰의 현재 재고를 Redis에 동기화
     * 
     * @param couponId 쿠폰 ID
     */
    @PostMapping("/{couponId}/initialize-stock")
    public CommonResponse<String> initializeCouponStock(@PathVariable Long couponId) {
        
        log.info("🔄 쿠폰 재고 초기화 요청: couponId={}", couponId);
        
        try {
            redisCouponService.initializeCouponStock(couponId);
            
            log.info("✅ 쿠폰 재고 초기화 완료: couponId={}", couponId);
            
            return CommonResponse.success("쿠폰 재고가 성공적으로 초기화되었습니다.");
            
        } catch (Exception e) {
            log.error("❌ 쿠폰 재고 초기화 실패: couponId={}, error={}", couponId, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 시스템 상태 조회 (모니터링 API)
     * 
     * 현재 처리 큐 상태와 시스템 헬스 체크
     * 
     * @return 시스템 상태 정보
     */
    @GetMapping("/system/status")
    public CommonResponse<Map<String, Object>> getSystemStatus() {
        
        try {
            Long queueSize = redisCouponService.getQueueSize();
            
            Map<String, Object> status = Map.of(
                "queueSize", queueSize != null ? queueSize : 0,
                "systemHealth", "OK",
                "timestamp", java.time.LocalDateTime.now()
            );
            
            log.debug("📊 시스템 상태 조회: queueSize={}", queueSize);
            
            return CommonResponse.success(status);
            
        } catch (Exception e) {
            log.error("❌ 시스템 상태 조회 실패", e);
            
            Map<String, Object> status = Map.of(
                "queueSize", -1,
                "systemHealth", "ERROR",
                "error", e.getMessage(),
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return CommonResponse.success(status);
        }
    }
    
    /**
     * 특정 쿠폰의 현재 재고 조회 (모니터링 API)
     * 
     * @param couponId 쿠폰 ID
     * @return 현재 Redis 재고 상태
     */
    @GetMapping("/{couponId}/stock")
    public CommonResponse<Map<String, Object>> getCouponStock(@PathVariable Long couponId) {
        
        try {
            Integer currentStock = redisCouponService.getCurrentStock(couponId);
            
            Map<String, Object> stockInfo = Map.of(
                "couponId", couponId,
                "currentStock", currentStock != null ? currentStock : "초기화 필요",
                "timestamp", java.time.LocalDateTime.now()
            );
            
            log.debug("📊 쿠폰 재고 조회: couponId={}, stock={}", couponId, currentStock);
            
            return CommonResponse.success(stockInfo);
            
        } catch (Exception e) {
            log.error("❌ 쿠폰 재고 조회 실패: couponId={}", couponId, e);
            throw e;
        }
    }
}