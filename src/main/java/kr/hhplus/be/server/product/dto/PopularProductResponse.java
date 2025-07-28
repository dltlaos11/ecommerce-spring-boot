package kr.hhplus.be.server.product.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인기 상품 정보")
public record PopularProductResponse(
                @Schema(description = "순위", example = "1") Integer rank,

                @Schema(description = "상품 ID", example = "1") Long productId,

                @Schema(description = "상품명", example = "고성능 노트북") String productName,

                @Schema(description = "상품 가격", example = "1500000.00") BigDecimal price,

                @Schema(description = "총 판매 수량", example = "150") Integer totalSalesQuantity,

                @Schema(description = "총 판매 금액", example = "225000000.00") BigDecimal totalSalesAmount) {
}