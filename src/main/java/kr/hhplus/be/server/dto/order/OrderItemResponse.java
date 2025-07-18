package kr.hhplus.be.server.dto.order;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

// 주문 상품 응답 
@Schema(description = "주문 상품 정보")
public class OrderItemResponse {

    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    @Schema(description = "주문 시점 상품명", example = "고성능 노트북")
    private String productName;

    @Schema(description = "주문 시점 상품가격", example = "1500000.00")
    private BigDecimal productPrice;

    @Schema(description = "주문 수량", example = "2")
    private Integer quantity;

    @Schema(description = "소계", example = "3000000.00")
    private BigDecimal subtotal;

    // 기본 생성자
    public OrderItemResponse() {
    }

    public OrderItemResponse(Long productId, String productName, BigDecimal productPrice,
            Integer quantity, BigDecimal subtotal) {
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    // Getters and Setters
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}