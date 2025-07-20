package kr.hhplus.be.server.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "주문 상품 요청")
public record OrderItemRequest(
        @NotNull(message = "상품 ID는 필수입니다.") @Schema(description = "상품 ID", example = "1") Long productId,

        @NotNull(message = "수량은 필수입니다.") @Min(value = 1, message = "수량은 1개 이상이어야 합니다.") @Schema(description = "주문 수량", example = "2", minimum = "1") Integer quantity) {
}