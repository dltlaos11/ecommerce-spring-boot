package kr.hhplus.be.server.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 정보")
public record OrderResponse(
        @Schema(description = "주문 ID", example = "1001") Long orderId,

        @Schema(description = "주문 번호", example = "ORD-20250716-001") String orderNumber,

        @Schema(description = "사용자 ID", example = "1") Long userId,

        @Schema(description = "총 주문 금액", example = "300000.00") BigDecimal totalAmount,

        @Schema(description = "할인 금액", example = "30000.00") BigDecimal discountAmount,

        @Schema(description = "최종 결제 금액", example = "270000.00") BigDecimal finalAmount,

        @Schema(description = "주문 상태", example = "COMPLETED") String status,

        @Schema(description = "주문 생성 시간", example = "2025-07-16T10:30:00") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,

        @Schema(description = "주문 상품 목록") List<OrderItemResponse> items) {
}