package kr.hhplus.be.server.ranking.controller;

import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.ranking.dto.ProductRankingResponse;
import kr.hhplus.be.server.ranking.service.ProductRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 상품 랭킹 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final ProductRankingService rankingService;

    /**
     * 일간 인기 상품 TOP 랭킹 조회
     * 
     * @param limit 조회할 상품 수 (기본값: 10)
     */
    @GetMapping("/products/daily")
    public CommonResponse<List<ProductRankingResponse>> getDailyTopProducts(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("📊 일간 TOP 랭킹 조회 요청: limit={}", limit);
        
        List<ProductRankingResponse> rankings = rankingService.getDailyTopProducts(limit);
        
        log.info("✅ 일간 TOP 랭킹 조회 완료: 조회된 상품 수={}", rankings.size());
        
        return CommonResponse.success(rankings);
    }

    /**
     * 3일 집계 인기 상품 TOP 랭킹 조회
     * 
     * @param limit 조회할 상품 수 (기본값: 10)
     */
    @GetMapping("/products/weekly")
    public CommonResponse<List<ProductRankingResponse>> getWeeklyTopProducts(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("📊 3일 집계 TOP 랭킹 조회 요청: limit={}", limit);
        
        List<ProductRankingResponse> rankings = rankingService.getWeeklyTopProducts(limit);
        
        log.info("✅ 3일 집계 TOP 랭킹 조회 완료: 조회된 상품 수={}", rankings.size());
        
        return CommonResponse.success(rankings);
    }

    /**
     * 특정 상품의 현재 랭킹 조회
     * 
     * @param productId 상품 ID
     */
    @GetMapping("/products/{productId}/rank")
    public CommonResponse<Long> getProductRank(@PathVariable Long productId) {
        
        log.info("📊 상품 랭킹 조회 요청: productId={}", productId);
        
        Long rank = rankingService.getProductRank(productId);
        
        if (rank != null) {
            log.info("✅ 상품 랭킹 조회 완료: productId={}, rank={}", productId, rank);
        } else {
            log.info("📊 상품 랭킹 데이터 없음: productId={}", productId);
        }
        
        return CommonResponse.success(rank);
    }

    /**
     * 랭킹 데이터 초기화 (관리자 기능)
     * 
     * @param date 초기화할 날짜 (YYYY-MM-DD 형식)
     */
    @DeleteMapping("/admin/clear")
    public CommonResponse<Void> clearRankingData(@RequestParam String date) {
        
        log.info("🗑️ 랭킹 데이터 초기화 요청: date={}", date);
        
        try {
            LocalDate targetDate = LocalDate.parse(date);
            rankingService.clearRankingData(targetDate);
            
            log.info("✅ 랭킹 데이터 초기화 완료: date={}", date);
            
            return CommonResponse.success(null);
            
        } catch (Exception e) {
            log.error("❌ 랭킹 데이터 초기화 실패: date={}", date, e);
            throw new IllegalArgumentException("잘못된 날짜 형식입니다: " + date);
        }
    }
}