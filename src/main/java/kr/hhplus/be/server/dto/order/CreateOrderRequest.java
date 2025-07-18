package kr.hhplus.be.server.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// 주문 생성 요청
@Schema(description = "주문 생성 요청")
public class CreateOrderRequest {
    
    @NotNull(message = "사용자 ID는 필수입니다.")
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    
    @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다.")
    @Valid
    @Schema(description = "주문 상품 목록")
    private List<OrderItemRequest> items;
    
    @Schema(description = "사용할 쿠폰 ID", example = "123")
    private Long couponId;
    
    // 기본 생성자
    public CreateOrderRequest() {}
    
    public CreateOrderRequest(Long userId, List<OrderItemRequest> items, Long couponId) {
        this.userId = userId;
        this.items = items;
        this.couponId = couponId;
    }
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public List<OrderItemRequest> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
    
    public Long getCouponId() {
        return couponId;
    }
    
    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }
}