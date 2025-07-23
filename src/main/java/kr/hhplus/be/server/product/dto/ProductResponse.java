package kr.hhplus.be.server.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 정보")
public record ProductResponse(
                @Schema(description = "상품 ID", example = "1") Long id,

                @Schema(description = "상품명", example = "고성능 노트북") String name,

                @Schema(description = "가격", example = "1500000.00") BigDecimal price,

                @Schema(description = "재고 수량", example = "10") Integer stockQuantity,

                @Schema(description = "생성 시간", example = "2025-07-16T10:30:00") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt) {
}