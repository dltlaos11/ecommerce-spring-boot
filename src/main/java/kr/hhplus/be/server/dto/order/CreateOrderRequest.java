package kr.hhplus.be.server.dto.order;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "주문 생성 요청")
public record CreateOrderRequest(
        @NotNull(message = "사용자 ID는 필수입니다.") @Schema(description = "사용자 ID", example = "1") Long userId,

        @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다.") @Valid @Schema(description = "주문 상품 목록") List<OrderItemRequest> items,

        @Schema(description = "사용할 쿠폰 ID", example = "123") Long couponId) {
}