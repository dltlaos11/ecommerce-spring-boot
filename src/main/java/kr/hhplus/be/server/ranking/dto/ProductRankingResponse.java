package kr.hhplus.be.server.ranking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 상품 랭킹 조회 응답 DTO
 */
public record ProductRankingResponse(
        Integer rank,
        Long productId,
        String productName,
        BigDecimal price,
        Integer stockQuantity,
        Long orderCount,
        Double score,
        LocalDateTime lastUpdated
) {
    
    public static ProductRankingResponse of(Integer rank, Long productId, String productName, 
                                          BigDecimal price, Integer stockQuantity, 
                                          Long orderCount, Double score) {
        return new ProductRankingResponse(
                rank, productId, productName, price, stockQuantity, 
                orderCount, score, LocalDateTime.now()
        );
    }
}