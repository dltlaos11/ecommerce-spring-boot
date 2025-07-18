package kr.hhplus.be.server.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;


// 주문 상품 요청
@Schema(description = "주문 상품 요청")
public class OrderItemRequest {
    
    @NotNull(message = "상품 ID는 필수입니다.")
    @Schema(description = "상품 ID", example = "1")
    private Long productId;
    
    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    @Schema(description = "주문 수량", example = "2", minimum = "1")
    private Integer quantity;
    
    // 기본 생성자
    public OrderItemRequest() {}
    
    public OrderItemRequest(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
    
    // Getters and Setters
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}