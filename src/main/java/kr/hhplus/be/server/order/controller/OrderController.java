package kr.hhplus.be.server.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.order.application.OrderUseCase; // UseCase 의존성 주입
import kr.hhplus.be.server.order.dto.CreateOrderRequest;
import kr.hhplus.be.server.order.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Application Layer 적용
 * 변경사항:
 * - OrderFacade → OrderUseCase 의존성 변경
 * - HTTP 요청/응답 처리에만 집중
 * - 비즈니스 로직은 UseCase에 위임
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "주문 관리", description = "주문 생성, 조회 API")
@RequiredArgsConstructor
public class OrderController {

  private final OrderUseCase orderUseCase; // UseCase 의존성 주입

  /**
   * 주문 생성
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "주문 생성", description = "상품을 주문하고 결제를 처리합니다. 쿠폰 적용 가능합니다.")
  public CommonResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    log.info("🛒 주문 생성 요청: userId = {}, 상품 {}개, 쿠폰 = {}",
        request.userId(), request.items().size(), request.couponId());

    // UseCase에 비즈니스 로직 위임
    OrderResponse response = orderUseCase.createOrder(request);

    log.info("✅ 주문 생성 완료: 주문번호 = {}, 최종금액 = {}",
        response.orderNumber(), response.finalAmount());

    return CommonResponse.success(response);
  }

  /**
   * 주문 상세 조회
   */
  @GetMapping("/{orderId}")
  @Operation(summary = "주문 상세 조회", description = "특정 주문의 상세 정보를 조회합니다.")
  public CommonResponse<OrderResponse> getOrder(
      @Parameter(description = "주문 ID", example = "1001", required = true) @PathVariable Long orderId) {

    log.info("🔍 주문 상세 조회 요청: orderId = {}", orderId);

    OrderResponse response = orderUseCase.getOrder(orderId);

    log.info("✅ 주문 상세 조회 완료: 주문번호 = {}", response.orderNumber());

    return CommonResponse.success(response);
  }

  /**
   * 사용자별 주문 목록 조회
   */
  @GetMapping("/users/{userId}")
  @Operation(summary = "사용자 주문 목록 조회", description = "특정 사용자의 모든 주문 목록을 조회합니다.")
  public CommonResponse<List<OrderResponse>> getUserOrders(
      @Parameter(description = "사용자 ID", example = "1", required = true) @PathVariable Long userId) {

    log.info("📋 사용자 주문 목록 조회 요청: userId = {}", userId);

    List<OrderResponse> responses = orderUseCase.getUserOrders(userId);

    log.info("✅ 사용자 주문 목록 조회 완료: userId = {}, {}개 주문", userId, responses.size());

    return CommonResponse.success(responses);
  }
}
