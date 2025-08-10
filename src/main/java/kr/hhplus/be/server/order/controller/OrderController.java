package kr.hhplus.be.server.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.order.application.CreateOrderUseCase;
import kr.hhplus.be.server.order.application.GetOrdersUseCase;
import kr.hhplus.be.server.order.dto.CreateOrderRequest;
import kr.hhplus.be.server.order.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "주문 관리", description = "주문 생성, 조회 API")
@RequiredArgsConstructor
public class OrderController {

  private final CreateOrderUseCase createOrderUseCase;
  private final GetOrdersUseCase getOrdersUseCase;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "주문 생성", description = "상품을 주문하고 결제를 처리합니다. 쿠폰 적용 가능합니다.")
  public CommonResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    OrderResponse response = createOrderUseCase.execute(request);

    return CommonResponse.success(response);
  }

  @GetMapping("/{orderId}")
  @Operation(summary = "주문 상세 조회", description = "특정 주문의 상세 정보를 조회합니다.")
  public CommonResponse<OrderResponse> getOrder(
      @Parameter(description = "주문 ID", example = "1001", required = true) @PathVariable Long orderId) {

    OrderResponse response = getOrdersUseCase.execute(orderId);

    return CommonResponse.success(response);
  }

  @GetMapping("/users/{userId}")
  @Operation(summary = "사용자 주문 목록 조회", description = "특정 사용자의 모든 주문 목록을 조회합니다.")
  public CommonResponse<List<OrderResponse>> getUserOrders(
      @Parameter(description = "사용자 ID", example = "1", required = true) @PathVariable Long userId) {


    List<OrderResponse> responses = getOrdersUseCase.executeUserOrders(userId);

    return CommonResponse.success(responses);
  }
}