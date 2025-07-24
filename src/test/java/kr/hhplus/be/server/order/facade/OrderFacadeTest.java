package kr.hhplus.be.server.order.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.balance.exception.InsufficientBalanceException;
import kr.hhplus.be.server.balance.service.BalanceService;
import kr.hhplus.be.server.coupon.dto.CouponValidationResponse;
import kr.hhplus.be.server.coupon.service.CouponService;
import kr.hhplus.be.server.dto.order.CreateOrderRequest;
import kr.hhplus.be.server.dto.order.OrderItemRequest;
import kr.hhplus.be.server.dto.order.OrderResponse;
import kr.hhplus.be.server.order.service.OrderService;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.product.exception.InsufficientStockException;
import kr.hhplus.be.server.product.service.ProductService;

/**
 * OrderFacade 단위 테스트
 * 
 * 테스트 전략:
 * - Mock을 활용한 여러 도메인 서비스 격리
 * - Facade 패턴의 복합 워크플로우 검증
 * - 성공/실패 시나리오 모두 검증
 * - 도메인 서비스 간 상호작용 검증
 * 
 * STEP05 테스트 범위:
 * - 주문 생성 워크플로우 (재고확인 → 쿠폰적용 → 결제 → 주문생성)
 * - 실패 시나리오 (재고부족, 잔액부족, 쿠폰오류)
 * - 도메인 서비스 호출 순서 및 횟수 검증
 * - 주문 조회 기능 (위임 패턴 검증)
 * 
 * STEP06 제외 기능:
 * - 주문 취소 및 보상 트랜잭션 (STEP06에서 구현)
 * - 동시성 제어 관련 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderFacade 단위 테스트 (STEP05)")
class OrderFacadeTest {

        @Mock
        private OrderService orderService;

        @Mock
        private ProductService productService;

        @Mock
        private BalanceService balanceService;

        @Mock
        private CouponService couponService;

        @InjectMocks
        private OrderFacade orderFacade;

        @Test
        @DisplayName("주문 생성 성공 - 쿠폰 없이 정상적인 주문이 처리된다")
        void 주문생성_성공_쿠폰없음() {
                // Given
                CreateOrderRequest request = createTestOrderRequest(1L, null);

                // Product Service Mock 설정
                ProductResponse product = createTestProductResponse(1L, "노트북", "1000000", 10);
                when(productService.getProduct(1L)).thenReturn(product);
                when(productService.hasEnoughStock(1L, 2)).thenReturn(true);

                // Balance Service Mock 설정
                when(balanceService.hasEnoughBalance(eq(1L), any(BigDecimal.class))).thenReturn(true);

                // Order Service Mock 설정
                OrderResponse expectedResponse = createTestOrderResponse(1L, "ORD-123", new BigDecimal("2000000"));
                when(orderService.createOrder(any(), any(), any(), any())).thenReturn(expectedResponse);

                // When
                OrderResponse response = orderFacade.createOrder(request);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.orderNumber()).isEqualTo("ORD-123");
                assertThat(response.finalAmount()).isEqualByComparingTo(new BigDecimal("2000000"));

                // 🔍 도메인 서비스 호출 검증
                verify(productService, times(2)).getProduct(1L); // 재고 확인 + 금액 계산
                verify(productService).hasEnoughStock(1L, 2);
                verify(balanceService).hasEnoughBalance(eq(1L), any(BigDecimal.class));
                verify(balanceService).deductBalance(eq(1L), any(BigDecimal.class), any(String.class));
                verify(productService).reduceStock(1L, 2);
                verify(orderService).createOrder(any(), any(), any(), any());

                // 쿠폰 서비스는 호출되지 않아야 함
                verify(couponService, never()).validateAndCalculateDiscount(any(), any(), any());
                verify(couponService, never()).useCoupon(any(), any(), any());
        }

        @Test
        @DisplayName("주문 생성 성공 - 쿠폰을 적용한 주문이 정상 처리된다")
        void 주문생성_성공_쿠폰적용() {
                // Given
                CreateOrderRequest request = createTestOrderRequest(1L, 101L); // 쿠폰 포함

                // Product Service Mock 설정
                ProductResponse product = createTestProductResponse(1L, "노트북", "1000000", 10);
                when(productService.getProduct(1L)).thenReturn(product);
                when(productService.hasEnoughStock(1L, 2)).thenReturn(true);

                // Coupon Service Mock 설정 (10% 할인)
                CouponValidationResponse couponValidation = new CouponValidationResponse(
                                101L, 1L, true, new BigDecimal("200000"), new BigDecimal("1800000"), null);
                when(couponService.validateAndCalculateDiscount(1L, 101L, new BigDecimal("2000000")))
                                .thenReturn(couponValidation);
                when(couponService.useCoupon(1L, 101L, new BigDecimal("2000000")))
                                .thenReturn(new BigDecimal("200000"));

                // Balance Service Mock 설정
                when(balanceService.hasEnoughBalance(eq(1L), any(BigDecimal.class))).thenReturn(true);

                // Order Service Mock 설정
                OrderResponse expectedResponse = createTestOrderResponse(1L, "ORD-123", new BigDecimal("1800000"));
                when(orderService.createOrder(any(), any(), any(), any())).thenReturn(expectedResponse);

                // When
                OrderResponse response = orderFacade.createOrder(request);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.finalAmount()).isEqualByComparingTo(new BigDecimal("1800000")); // 할인 적용됨

                // 🔍 쿠폰 관련 서비스 호출 검증
                verify(couponService).validateAndCalculateDiscount(1L, 101L, new BigDecimal("2000000"));
                verify(couponService).useCoupon(1L, 101L, new BigDecimal("2000000"));
        }

        @Test
        @DisplayName("주문 생성 실패 - 재고가 부족할 때 예외가 발생한다")
        void 주문생성_실패_재고부족() {
                // Given
                CreateOrderRequest request = createTestOrderRequest(1L, null);

                ProductResponse product = createTestProductResponse(1L, "노트북", "1000000", 1); // 재고 1개
                when(productService.getProduct(1L)).thenReturn(product);
                when(productService.hasEnoughStock(1L, 2)).thenReturn(false); // 재고 부족

                // When & Then
                assertThatThrownBy(() -> orderFacade.createOrder(request))
                                .isInstanceOf(InsufficientStockException.class);

                // 🔍 재고 검증 후 중단되므로 후속 서비스들은 호출되지 않아야 함
                verify(productService).hasEnoughStock(1L, 2);
                verify(balanceService, never()).hasEnoughBalance(any(), any());
                verify(orderService, never()).createOrder(any(), any(), any(), any());
        }

        @Test
        @DisplayName("주문 생성 실패 - 잔액이 부족할 때 예외가 발생한다")
        void 주문생성_실패_잔액부족() {
                // Given
                CreateOrderRequest request = createTestOrderRequest(1L, null);

                ProductResponse product = createTestProductResponse(1L, "노트북", "1000000", 10);
                when(productService.getProduct(1L)).thenReturn(product);
                when(productService.hasEnoughStock(1L, 2)).thenReturn(true);
                when(balanceService.hasEnoughBalance(eq(1L), any(BigDecimal.class))).thenReturn(false); // 잔액 부족

                // When & Then
                assertThatThrownBy(() -> orderFacade.createOrder(request))
                                .isInstanceOf(InsufficientBalanceException.class);

                // 🔍 잔액 검증 후 중단되므로 주문 생성은 호출되지 않아야 함
                verify(balanceService).hasEnoughBalance(eq(1L), any(BigDecimal.class));
                verify(productService, never()).reduceStock(anyLong(), anyInt());
                verify(orderService, never()).createOrder(any(), any(), any(), any());
        }

        @Test
        @DisplayName("주문 생성 실패 - 사용할 수 없는 쿠폰일 때 예외가 발생한다")
        void 주문생성_실패_쿠폰사용불가() {
                // Given
                CreateOrderRequest request = createTestOrderRequest(1L, 101L);

                ProductResponse product = createTestProductResponse(1L, "노트북", "1000000", 10);
                when(productService.getProduct(1L)).thenReturn(product);
                when(productService.hasEnoughStock(1L, 2)).thenReturn(true);

                // 쿠폰 사용 불가
                CouponValidationResponse couponValidation = new CouponValidationResponse(
                                101L, 1L, false, BigDecimal.ZERO, new BigDecimal("2000000"), "쿠폰이 만료되었습니다.");
                when(couponService.validateAndCalculateDiscount(1L, 101L, new BigDecimal("2000000")))
                                .thenReturn(couponValidation);

                // When & Then
                assertThatThrownBy(() -> orderFacade.createOrder(request))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("쿠폰을 사용할 수 없습니다");

                // 🔍 쿠폰 검증 후 중단되므로 결제는 호출되지 않아야 함
                verify(couponService).validateAndCalculateDiscount(1L, 101L, new BigDecimal("2000000"));
                verify(balanceService, never()).deductBalance(any(), any(), any());
        }

        @Test
        @DisplayName("주문 상세 조회 성공 - OrderService에 위임된다")
        void 주문상세조회_성공() {
                // Given
                Long orderId = 1001L;
                OrderResponse expectedResponse = createTestOrderResponse(1L, "ORD-123", new BigDecimal("2000000"));
                when(orderService.getOrder(orderId)).thenReturn(expectedResponse);

                // When
                OrderResponse response = orderFacade.getOrder(orderId);

                // Then
                assertThat(response).isEqualTo(expectedResponse);
                verify(orderService).getOrder(orderId);
        }

        @Test
        @DisplayName("사용자 주문 목록 조회 성공 - OrderService에 위임된다")
        void 사용자주문목록조회_성공() {
                // Given
                Long userId = 1L;
                List<OrderResponse> expectedResponses = List.of(
                                createTestOrderResponse(1L, "ORD-123", new BigDecimal("2000000")),
                                createTestOrderResponse(1L, "ORD-124", new BigDecimal("1500000")));
                when(orderService.getUserOrders(userId)).thenReturn(expectedResponses);

                // When
                List<OrderResponse> responses = orderFacade.getUserOrders(userId);

                // Then
                assertThat(responses).hasSize(2);
                assertThat(responses).isEqualTo(expectedResponses);
                verify(orderService).getUserOrders(userId);
        }

        @Test
        @DisplayName("도메인 서비스 호출 순서 검증 - 정확한 워크플로우 순서로 호출된다")
        void 도메인서비스호출순서_검증() {
                // Given
                CreateOrderRequest request = createTestOrderRequest(1L, null);

                ProductResponse product = createTestProductResponse(1L, "노트북", "1000000", 10);
                when(productService.getProduct(1L)).thenReturn(product);
                when(productService.hasEnoughStock(1L, 2)).thenReturn(true);
                when(balanceService.hasEnoughBalance(eq(1L), any(BigDecimal.class))).thenReturn(true);

                OrderResponse expectedResponse = createTestOrderResponse(1L, "ORD-123", new BigDecimal("2000000"));
                when(orderService.createOrder(any(), any(), any(), any())).thenReturn(expectedResponse);

                // When
                orderFacade.createOrder(request);

                // Then - 호출 순서 검증
                var inOrder = inOrder(productService, balanceService, orderService);

                // 1. 재고 검증
                inOrder.verify(productService).hasEnoughStock(1L, 2);

                // 2. 잔액 검증 및 차감
                inOrder.verify(balanceService).hasEnoughBalance(eq(1L), any(BigDecimal.class));
                inOrder.verify(balanceService).deductBalance(eq(1L), any(BigDecimal.class), any(String.class));

                // 3. 재고 차감
                inOrder.verify(productService).reduceStock(1L, 2);

                // 4. 주문 생성
                inOrder.verify(orderService).createOrder(any(), any(), any(), any());
        }

        @Test
        @DisplayName("복합 도메인 워크플로우 검증 - 모든 단계가 순차적으로 실행된다")
        void 복합도메인워크플로우_검증() {
                // Given
                CreateOrderRequest request = createTestOrderRequest(1L, 201L); // 쿠폰 포함

                // 모든 서비스 Mock 설정
                ProductResponse product = createTestProductResponse(1L, "노트북", "1000000", 10);
                when(productService.getProduct(1L)).thenReturn(product);
                when(productService.hasEnoughStock(1L, 2)).thenReturn(true);

                CouponValidationResponse couponValidation = new CouponValidationResponse(
                                201L, 1L, true, new BigDecimal("100000"), new BigDecimal("1900000"), null);
                when(couponService.validateAndCalculateDiscount(eq(1L), eq(201L), any(BigDecimal.class)))
                                .thenReturn(couponValidation);
                when(couponService.useCoupon(eq(1L), eq(201L), any(BigDecimal.class)))
                                .thenReturn(new BigDecimal("100000"));

                when(balanceService.hasEnoughBalance(eq(1L), any(BigDecimal.class))).thenReturn(true);

                OrderResponse expectedResponse = createTestOrderResponse(1L, "ORD-123", new BigDecimal("1900000"));
                when(orderService.createOrder(any(), any(), any(), any())).thenReturn(expectedResponse);

                // When
                OrderResponse response = orderFacade.createOrder(request);

                // Then
                assertThat(response.finalAmount()).isEqualByComparingTo(new BigDecimal("1900000"));

                // 🔍 전체 워크플로우 검증: 재고 → 쿠폰 → 잔액 → 주문
                verify(productService).hasEnoughStock(1L, 2); // 1단계: 재고 검증
                verify(couponService).validateAndCalculateDiscount(eq(1L), eq(201L), any(BigDecimal.class)); // 2단계: 쿠폰
                                                                                                             // 검증
                verify(balanceService).deductBalance(eq(1L), any(BigDecimal.class), any(String.class)); // 3단계: 결제
                verify(productService).reduceStock(1L, 2); // 4단계: 재고 차감
                verify(couponService).useCoupon(eq(1L), eq(201L), any(BigDecimal.class)); // 5단계: 쿠폰 사용
                verify(orderService).createOrder(any(), any(), any(), any()); // 6단계: 주문 생성
        }

        // ==================== 테스트 데이터 생성 헬퍼 메서드들 ====================

        private CreateOrderRequest createTestOrderRequest(Long userId, Long couponId) {
                List<OrderItemRequest> items = List.of(
                                new OrderItemRequest(1L, 2) // 상품 ID 1, 수량 2
                );
                return new CreateOrderRequest(userId, items, couponId);
        }

        private ProductResponse createTestProductResponse(Long id, String name, String price, Integer stock) {
                return new ProductResponse(id, name, new BigDecimal(price), stock, LocalDateTime.now());
        }

        private OrderResponse createTestOrderResponse(Long userId, String orderNumber, BigDecimal finalAmount) {
                return new OrderResponse(
                                1001L, // orderId
                                orderNumber,
                                userId,
                                new BigDecimal("2000000"), // totalAmount
                                BigDecimal.ZERO, // discountAmount (기본값)
                                finalAmount,
                                "COMPLETED",
                                LocalDateTime.now(),
                                List.of() // items
                );
        }
}