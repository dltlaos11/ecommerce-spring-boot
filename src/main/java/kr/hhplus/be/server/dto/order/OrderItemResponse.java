package kr.hhplus.be.server.dto.order;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 상품 정보")
public record OrderItemResponse(
        @Schema(description = "상품 ID", example = "1") Long productId,

        @Schema(description = "주문 시점 상품명", example = "고성능 노트북") String productName,

        @Schema(description = "주문 시점 상품가격", example = "1500000.00") BigDecimal productPrice,

        @Schema(description = "주문 수량", example = "2") Integer quantity,

        @Schema(description = "소계", example = "3000000.00") BigDecimal subtotal) {
}