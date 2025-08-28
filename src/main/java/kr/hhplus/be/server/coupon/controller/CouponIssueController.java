package kr.hhplus.be.server.coupon.controller;

import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.coupon.dto.AsyncCouponIssueRequest;
import kr.hhplus.be.server.coupon.dto.AsyncCouponIssueResponse;
import kr.hhplus.be.server.coupon.dto.SystemStatusResponse;
import kr.hhplus.be.server.coupon.dto.CouponStockResponse;
import kr.hhplus.be.server.coupon.service.RedisCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 쿠폰 발급 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/coupons/async")
@RequiredArgsConstructor
public class CouponIssueController {
    
    private final RedisCouponService redisCouponService;
    
    @PostMapping("/issue")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CommonResponse<AsyncCouponIssueResponse> issueCouponAsync(
            @Valid @RequestBody AsyncCouponIssueRequest request) {
        
        log.info("🎫 비동기 쿠폰 발급 API 호출: userId={}, couponId={}", 
                request.userId(), request.couponId());
        
        try {
            AsyncCouponIssueResponse response = redisCouponService.requestCouponIssueAsync(request);
            
            log.info("✅ 비동기 쿠폰 발급 요청 접수: requestId={}", response.requestId());
            
            return CommonResponse.success(response);
            
        } catch (Exception e) {
            log.error("❌ 비동기 쿠폰 발급 요청 실패: userId={}, couponId={}, error={}", 
                    request.userId(), request.couponId(), e.getMessage());
            throw e;
        }
    }
    
    @GetMapping("/status/{requestId}")
    public CommonResponse<AsyncCouponIssueResponse> getIssueStatus(@PathVariable String requestId) {
        
        log.debug("📊 쿠폰 발급 상태 조회: requestId={}", requestId);
        
        try {
            AsyncCouponIssueResponse response = redisCouponService.getRequestStatus(requestId);
            
            log.debug("✅ 상태 조회 완료: requestId={}, status={}", requestId, response.status());
            
            return CommonResponse.success(response);
            
        } catch (Exception e) {
            log.error("❌ 상태 조회 실패: requestId={}, error={}", requestId, e.getMessage());
            throw e;
        }
    }
    
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
    
    @GetMapping("/system/status")
    public CommonResponse<SystemStatusResponse> getSystemStatus() {
        
        try {
            Long queueSize = redisCouponService.getQueueSize();
            SystemStatusResponse status = SystemStatusResponse.ok(queueSize);
            
            log.debug("📊 시스템 상태 조회: queueSize={}", queueSize);
            
            return CommonResponse.success(status);
            
        } catch (Exception e) {
            log.error("❌ 시스템 상태 조회 실패", e);
            SystemStatusResponse status = SystemStatusResponse.error(e.getMessage());
            
            return CommonResponse.success(status);
        }
    }
    
    @GetMapping("/{couponId}/stock")
    public CommonResponse<CouponStockResponse> getCouponStock(@PathVariable Long couponId) {
        
        try {
            Integer currentStock = redisCouponService.getCurrentStock(couponId);
            CouponStockResponse stockInfo = CouponStockResponse.of(couponId, currentStock);
            
            log.debug("📊 쿠폰 재고 조회: couponId={}, stock={}", couponId, currentStock);
            
            return CommonResponse.success(stockInfo);
            
        } catch (Exception e) {
            log.error("❌ 쿠폰 재고 조회 실패: couponId={}", couponId, e);
            throw e;
        }
    }
}