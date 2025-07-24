// OrderController.java
package kr.hhplus.be.server.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.dto.order.CreateOrderRequest;
import kr.hhplus.be.server.dto.order.OrderResponse;
import kr.hhplus.be.server.order.facade.OrderFacade;
import lombok.extern.slf4j.Slf4j;

/**
 * 변경사항:
 * - OrderFacade 연동 (여러 도메인 서비스 조합)
 * - HTTP 요청/응답 처리에만 집중
 * - 복잡한 비즈니스 로직은 Facade에 위임
 * 
 * 책임:
 * - HTTP 요청 파라미터 검증
 * - OrderFacade 호출 및 결과 반환
 * - 예외 처리는 GlobalExceptionHandler에 위임
 * - REST API 문서화 (Swagger)
 * 
 * STEP06 제외 기능:
 * - 주문 취소 API (보상 트랜잭션)
 * - 동시성 제어 관련 엔드포인트
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "주문 관리", description = "주문 생성, 조회 API")
public class OrderController {

  private final OrderFacade orderFacade;

  public OrderController(OrderFacade orderFacade) {
    this.orderFacade = orderFacade;
  }

  /**
   * 주문 생성
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "주문 생성", description = "상품을 주문하고 결제를 처리합니다. 쿠폰 적용 가능합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "주문 생성 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "주문 생성 성공", value = """
          {
            "success": true,
            "data": {
              "orderId": 1001,
              "orderNumber": "ORD-20250725-A1B2C3D4",
              "userId": 1,
              "totalAmount": 3200000.00,
              "discountAmount": 320000.00,
              "finalAmount": 2880000.00,
              "status": "COMPLETED",
              "createdAt": "2025-07-25T10:30:00",
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
            "timestamp": "2025-07-25T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "400", description = "잘못된 주문 요청", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "주문 상품 없음", value = """
              {
                "success": false,
                "error": {
                  "code": "ORDER_ITEMS_EMPTY",
                  "message": "주문 상품은 최소 1개 이상이어야 합니다."
                },
                "timestamp": "2025-07-25T10:30:00"
              }
              """),
          @ExampleObject(name = "재고 부족", value = """
              {
                "success": false,
                "error": {
                  "code": "INSUFFICIENT_STOCK",
                  "message": "재고가 부족합니다."
                },
                "timestamp": "2025-07-25T10:30:00"
              }
              """),
          @ExampleObject(name = "잔액 부족", value = """
              {
                "success": false,
                "error": {
                  "code": "INSUFFICIENT_BALANCE",
                  "message": "잔액이 부족합니다."
                },
                "timestamp": "2025-07-25T10:30:00"
              }
              """)
      })),
      @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "상품 없음", value = """
              {
                "success": false,
                "error": {
                  "code": "PRODUCT_NOT_FOUND",
                  "message": "상품을 찾을 수 없습니다."
                },
                "timestamp": "2025-07-25T10:30:00"
              }
              """),
          @ExampleObject(name = "쿠폰 없음", value = """
              {
                "success": false,
                "error": {
                  "code": "COUPON_NOT_FOUND",
                  "message": "쿠폰을 찾을 수 없습니다."
                },
                "timestamp": "2025-07-25T10:30:00"
              }
              """)
      })),
      @ApiResponse(responseCode = "409", description = "비즈니스 규칙 위반", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "쿠폰 사용 불가", value = """
              {
                "success": false,
                "error": {
                  "code": "COUPON_NOT_APPLICABLE",
                  "message": "적용할 수 없는 쿠폰입니다."
                },
                "timestamp": "2025-07-25T10:30:00"
              }
              """)
      })),
      @ApiResponse(responseCode = "500", description = "결제 처리 오류", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "결제 실패", value = """
          {
            "success": false,
            "error": {
              "code": "PAYMENT_FAILED",
              "message": "결제 처리 중 오류가 발생했습니다."
            },
            "timestamp": "2025-07-25T10:30:00"
          }
          """)))
  })
  public CommonResponse<OrderResponse> createOrder(
      @Valid @RequestBody CreateOrderRequest request) {

    log.info("🛒 주문 생성 요청: userId = {}, 상품 {}개, 쿠폰 = {}",
        request.userId(), request.items().size(), request.couponId());

    OrderResponse response = orderFacade.createOrder(request);

    log.info("✅ 주문 생성 완료: 주문번호 = {}, 최종금액 = {}",
        response.orderNumber(), response.finalAmount());

    return CommonResponse.success(response);
  }

  /**
   * 주문 상세 조회
   */
  @GetMapping("/{orderId}")
  @Operation(summary = "주문 상세 조회", description = "특정 주문의 상세 정보를 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "주문 상세 조회 성공", value = """
          {
            "success": true,
            "data": {
              "orderId": 1001,
              "orderNumber": "ORD-20250725-A1B2C3D4",
              "userId": 1,
              "totalAmount": 3200000.00,
              "discountAmount": 320000.00,
              "finalAmount": 2880000.00,
              "status": "COMPLETED",
              "createdAt": "2025-07-25T10:30:00",
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
            "timestamp": "2025-07-25T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "주문 없음", value = """
          {
            "success": false,
            "error": {
              "code": "ORDER_NOT_FOUND",
              "message": "주문을 찾을 수 없습니다."
            },
            "timestamp": "2025-07-25T10:30:00"
          }
          """)))
  })
  public CommonResponse<OrderResponse> getOrder(
      @Parameter(description = "주문 ID", example = "1001", required = true) @PathVariable Long orderId) {

    log.info("🔍 주문 상세 조회 요청: orderId = {}", orderId);

    OrderResponse response = orderFacade.getOrder(orderId);

    log.info("✅ 주문 상세 조회 완료: 주문번호 = {}", response.orderNumber());

    return CommonResponse.success(response);
  }

  /**
   * 사용자별 주문 목록 조회
   */
  @GetMapping("/users/{userId}")
  @Operation(summary = "사용자 주문 목록 조회", description = "특정 사용자의 모든 주문 목록을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "사용자 주문 목록", value = """
          {
            "success": true,
            "data": [
              {
                "orderId": 1001,
                "orderNumber": "ORD-20250725-A1B2C3D4",
                "userId": 1,
                "totalAmount": 3200000.00,
                "discountAmount": 320000.00,
                "finalAmount": 2880000.00,
                "status": "COMPLETED",
                "createdAt": "2025-07-25T10:30:00",
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
              {
                "orderId": 1002,
                "orderNumber": "ORD-20250724-B5C6D7E8",
                "userId": 1,
                "totalAmount": 150000.00,
                "discountAmount": 0.00,
                "finalAmount": 150000.00,
                "status": "COMPLETED",
                "createdAt": "2025-07-24T14:20:00",
                "items": [
                  {
                    "productId": 3,
                    "productName": "기계식 키보드",
                    "productPrice": 150000.00,
                    "quantity": 1,
                    "subtotal": 150000.00
                  }
                ]
              }
            ],
            "timestamp": "2025-07-25T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "잘못된 사용자 ID", value = """
          {
            "success": false,
            "error": {
              "code": "INVALID_PARAMETER",
              "message": "잘못된 요청 파라미터입니다."
            },
            "timestamp": "2025-07-25T10:30:00"
          }
          """)))
  })
  public CommonResponse<List<OrderResponse>> getUserOrders(
      @Parameter(description = "사용자 ID", example = "1", required = true) @PathVariable Long userId) {

    log.info("📋 사용자 주문 목록 조회 요청: userId = {}", userId);

    List<OrderResponse> responses = orderFacade.getUserOrders(userId);

    log.info("✅ 사용자 주문 목록 조회 완료: userId = {}, {}개 주문", userId, responses.size());

    return CommonResponse.success(responses);
  }
}