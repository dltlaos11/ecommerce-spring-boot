package kr.hhplus.be.server.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.dto.order.CreateOrderRequest;
import kr.hhplus.be.server.dto.order.OrderItemRequest;
import kr.hhplus.be.server.dto.order.OrderItemResponse;
import kr.hhplus.be.server.dto.order.OrderResponse;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "주문 관리", description = "주문 생성 및 조회 API")
public class OrderController {

  @PostMapping
  @Operation(summary = "주문 생성 및 결제", description = "상품을 주문하고 결제를 처리합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "주문 성공", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = """
          {
            "success": true,
            "data": {
              "orderId": 1001,
              "orderNumber": "ORD-20250717-001",
              "userId": 1,
              "totalAmount": 3050000.00,
              "discountAmount": 305000.00,
              "finalAmount": 2745000.00,
              "status": "COMPLETED",
              "createdAt": "2025-07-17T10:30:00",
              "items": [
                {
                  "productId": 1,
                  "productName": "고성능 노트북",
                  "productPrice": 1500000.00,
                  "quantity": 2,
                  "subtotal": 3000000.00
                },
                {
                  "productId": 2,
                  "productName": "무선 마우스",
                  "productPrice": 50000.00,
                  "quantity": 1,
                  "subtotal": 50000.00
                }
              ]
            },
            "error": null,
            "timestamp": "2025-07-17T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "400", description = "잘못된 주문 정보", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = """
          {
            "success": false,
            "data": null,
            "error": {
              "code": "VALIDATION_ERROR",
              "message": "주문 상품은 최소 1개 이상이어야 합니다."
            },
            "timestamp": "2025-07-17T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "409", description = "재고 부족 또는 잔액 부족", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = """
          {
            "success": false,
            "data": null,
            "error": {
              "code": "INSUFFICIENT_STOCK",
              "message": "재고가 부족합니다."
            },
            "timestamp": "2025-07-17T10:30:00"
          }
          """)))
  })
  @ResponseStatus(HttpStatus.CREATED)
  public CommonResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {

    BigDecimal totalAmount = calculateTotalAmount(request.getItems());
    BigDecimal discountAmount = request.getCouponId() != null ? totalAmount.multiply(new BigDecimal("0.1"))
        : BigDecimal.ZERO;
    BigDecimal finalAmount = totalAmount.subtract(discountAmount);

    // 주문 상품 목록 Mock 데이터 생성
    List<OrderItemResponse> items = request.getItems().stream()
        .map(item -> {
          String productName = getProductName(item.getProductId());
          BigDecimal productPrice = getProductPrice(item.getProductId());
          BigDecimal subtotal = productPrice.multiply(new BigDecimal(item.getQuantity()));

          return new OrderItemResponse(
              item.getProductId(),
              productName,
              productPrice,
              item.getQuantity(),
              subtotal);
        })
        .toList();

    OrderResponse response = new OrderResponse(
        1001L,
        "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-001",
        request.getUserId(),
        totalAmount,
        discountAmount,
        finalAmount,
        "COMPLETED",
        LocalDateTime.now(),
        items // order_items
    );

    return CommonResponse.success(response);
  }

  // 주문 상세 조회
  @GetMapping("/{orderId}")
  @Operation(summary = "주문 상세 조회", description = "특정 주문의 상세 정보를 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = """
          {
            "success": true,
            "data": {
              "orderId": 1001,
              "orderNumber": "ORD-20250717-001",
              "userId": 1,
              "totalAmount": 3050000.00,
              "discountAmount": 305000.00,
              "finalAmount": 2745000.00,
              "status": "COMPLETED",
              "createdAt": "2025-07-17T08:30:00",
              "items": [
                {
                  "productId": 1,
                  "productName": "고성능 노트북",
                  "productPrice": 1500000.00,
                  "quantity": 2,
                  "subtotal": 3000000.00
                }
              ]
            },
            "error": null,
            "timestamp": "2025-07-17T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = """
          {
            "success": false,
            "data": null,
            "error": {
              "code": "ORDER_NOT_FOUND",
              "message": "주문을 찾을 수 없습니다."
            },
            "timestamp": "2025-07-17T10:30:00"
          }
          """)))
  })
  public CommonResponse<OrderResponse> getOrder(
      @Parameter(description = "주문 ID", example = "1001") @PathVariable Long orderId) {

    // Mock 주문 상품 목록
    List<OrderItemResponse> items = List.of(
        new OrderItemResponse(1L, "고성능 노트북", new BigDecimal("1500000.00"), 2, new BigDecimal("3000000.00")),
        new OrderItemResponse(2L, "무선 마우스", new BigDecimal("50000.00"), 1, new BigDecimal("50000.00")));

    OrderResponse response = new OrderResponse(
        orderId,
        "ORD-20250716-001",
        1L,
        new BigDecimal("3050000.00"),
        new BigDecimal("305000.00"),
        new BigDecimal("2745000.00"),
        "COMPLETED",
        LocalDateTime.now().minusHours(2),
        items);

    return CommonResponse.success(response);
  }

  // OrderItemRequest의 데이터는 내부적으로만 활용
  private BigDecimal calculateTotalAmount(java.util.List<OrderItemRequest> items) {
    return items.stream()
        .map(item -> {
          BigDecimal price = getProductPrice(item.getProductId());
          return price.multiply(new BigDecimal(item.getQuantity()));
        })
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal getProductPrice(Long productId) {
    return switch (productId.intValue()) {
      case 1 -> new BigDecimal("1500000.00"); // 노트북
      case 2 -> new BigDecimal("50000.00"); // 마우스
      case 3 -> new BigDecimal("150000.00"); // 키보드
      case 4 -> new BigDecimal("300000.00"); // 모니터
      case 5 -> new BigDecimal("80000.00"); // 웹캠
      default -> new BigDecimal("100000.00"); // 기본값
    };
  }

  private String getProductName(Long productId) {
    return switch (productId.intValue()) {
      case 1 -> "고성능 노트북";
      case 2 -> "무선 마우스";
      case 3 -> "기계식 키보드";
      case 4 -> "모니터";
      case 5 -> "웹캠";
      default -> "상품명";
    };
  }
}