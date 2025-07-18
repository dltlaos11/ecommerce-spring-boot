package kr.hhplus.be.server.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 정보")
public class OrderResponse {

    @Schema(description = "주문 ID", example = "1001")
    private Long orderId;

    @Schema(description = "주문 번호", example = "ORD-20250716-001")
    private String orderNumber;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "총 주문 금액", example = "300000.00")
    private BigDecimal totalAmount;

    @Schema(description = "할인 금액", example = "30000.00")
    private BigDecimal discountAmount;

    @Schema(description = "최종 결제 금액", example = "270000.00")
    private BigDecimal finalAmount;

    @Schema(description = "주문 상태", example = "COMPLETED")
    private String status;

    @Schema(description = "주문 생성 시간", example = "2025-07-16T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "주문 상품 목록")
    private List<OrderItemResponse> items;

    // 기본 생성자
    public OrderResponse() {
    }

    // 파라미터 생성자
    public OrderResponse(Long orderId, String orderNumber, Long userId, BigDecimal totalAmount,
            BigDecimal discountAmount, BigDecimal finalAmount, String status,
            LocalDateTime createdAt, List<OrderItemResponse> items) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.items = items;
    }

    // 모든 getter 메서드 추가 (Jackson 직렬화를 위해 필수!)
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
    }
}