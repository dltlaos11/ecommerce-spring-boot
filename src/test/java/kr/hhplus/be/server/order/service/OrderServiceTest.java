package kr.hhplus.be.server.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.Payment;
import kr.hhplus.be.server.order.dto.CreateOrderRequest;
import kr.hhplus.be.server.order.dto.OrderItemRequest;
import kr.hhplus.be.server.order.dto.OrderResponse;
import kr.hhplus.be.server.order.exception.OrderNotFoundException;
import kr.hhplus.be.server.order.repository.OrderItemRepository;
import kr.hhplus.be.server.order.repository.OrderRepository;
import kr.hhplus.be.server.order.repository.PaymentRepository;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.product.service.ProductService;

/**
 * OrderService 단위 테스트
 * 
 * 테스트 전략:
 * - Mock을 활용한 외부 의존성 격리
 * - 복잡한 주문 생성 워크플로우 검증
 * - N+1 문제 해결 로직 검증
 * - 트랜잭션 경계 및 데이터 일관성 확인
 * 
 * 핵심 비즈니스 로직:
 * - 주문 생성 (상품 정보 포함)
 * - 주문 조회 (단건 및 목록)
 * - 주문 상태 관리
 * - 결제 정보 연동
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("주문 생성 성공 - 상품 정보를 포함한 주문이 정상적으로 생성된다")
    void 주문생성_성공() {
        // Given
        Long userId = 1L;
        BigDecimal totalAmount = new BigDecimal("150000");
        BigDecimal discountAmount = new BigDecimal("15000");
        BigDecimal finalAmount = new BigDecimal("135000");

        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                List.of(new OrderItemRequest(1L, 2)),
                1L // 쿠폰 ID
        );

        // 상품 정보 맵
        Map<Long, ProductResponse> productInfoMap = Map.of(
                1L, new ProductResponse(1L, "테스트 노트북", new BigDecimal("75000"), 10, LocalDateTime.now()));

        // Mock 설정
        Order mockOrder = createTestOrder(1L, "ORD-TEST-001", userId, totalAmount, discountAmount, finalAmount);
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        OrderItem mockOrderItem = createTestOrderItem(1L, 1L, 1L, "테스트 노트북", new BigDecimal("75000"), 2);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(mockOrderItem);

        Payment mockPayment = createTestPayment(1L, 1L, userId, finalAmount);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // When
        OrderResponse response = orderService.createOrderWithProductInfo(
                request, totalAmount, discountAmount, finalAmount, productInfoMap);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.orderNumber()).isEqualTo("ORD-TEST-001");
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.totalAmount()).isEqualByComparingTo(totalAmount);
        assertThat(response.discountAmount()).isEqualByComparingTo(discountAmount);
        assertThat(response.finalAmount()).isEqualByComparingTo(finalAmount);
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.items()).hasSize(1);

        // Mock 호출 검증
        verify(orderRepository, times(2)).save(any(Order.class)); // 생성 + 완료 처리로 2번 호출
        verify(orderItemRepository).save(any(OrderItem.class));
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("주문 조회 성공 - 존재하는 주문을 정상적으로 조회한다")
    void 주문조회_성공() {
        // Given
        Long orderId = 1L;
        Order mockOrder = createTestOrder(orderId, "ORD-TEST-001", 1L,
                new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("100000"));

        List<OrderItem> mockOrderItems = List.of(
                createTestOrderItem(1L, orderId, 1L, "상품1", new BigDecimal("50000"), 1),
                createTestOrderItem(2L, orderId, 2L, "상품2", new BigDecimal("50000"), 1));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(mockOrderItems);

        // When
        OrderResponse response = orderService.getOrder(orderId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.orderNumber()).isEqualTo("ORD-TEST-001");
        assertThat(response.items()).hasSize(2);
        assertThat(response.items())
                .extracting(item -> item.productName())
                .containsExactly("상품1", "상품2");

        verify(orderRepository).findById(orderId);
        verify(orderItemRepository).findByOrderId(orderId);
    }

    @Test
    @DisplayName("주문 조회 실패 - 존재하지 않는 주문 조회 시 예외가 발생한다")
    void 주문조회_실패_주문없음() {
        // Given
        Long nonExistentOrderId = 999L;
        when(orderRepository.findById(nonExistentOrderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrder(nonExistentOrderId))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository).findById(nonExistentOrderId);
        verify(orderItemRepository, never()).findByOrderId(any());
    }

    @Test
    @DisplayName("사용자 주문 목록 조회 성공 - N+1 문제가 해결되어야 한다")
    void 사용자주문목록조회_성공_N1문제해결() {
        // Given
        Long userId = 1L;
        List<Order> mockOrders = List.of(
                createTestOrder(1L, "ORD-001", userId, new BigDecimal("100000"), BigDecimal.ZERO,
                        new BigDecimal("100000")),
                createTestOrder(2L, "ORD-002", userId, new BigDecimal("200000"), BigDecimal.ZERO,
                        new BigDecimal("200000")));

        List<OrderItem> mockAllOrderItems = List.of(
                createTestOrderItem(1L, 1L, 1L, "상품1", new BigDecimal("100000"), 1),
                createTestOrderItem(2L, 2L, 2L, "상품2", new BigDecimal("200000"), 1));

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(mockOrders);
        when(orderItemRepository.findByOrderIdIn(anyList())).thenReturn(mockAllOrderItems);

        // When
        List<OrderResponse> responses = orderService.getUserOrders(userId);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(OrderResponse::orderNumber)
                .containsExactly("ORD-001", "ORD-002");

        // N+1 문제 해결 검증
        verify(orderRepository).findByUserIdOrderByCreatedAtDesc(userId);
        verify(orderItemRepository).findByOrderIdIn(List.of(1L, 2L)); // 한 번에 배치 조회
        verify(orderItemRepository, never()).findByOrderId(any()); // 개별 조회는 없어야 함
    }

    @Test
    @DisplayName("주문 번호로 조회 성공")
    void 주문번호로조회_성공() {
        // Given
        String orderNumber = "ORD-TEST-001";
        Order mockOrder = createTestOrder(1L, orderNumber, 1L,
                new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("100000"));

        List<OrderItem> mockOrderItems = List.of(
                createTestOrderItem(1L, 1L, 1L, "상품1", new BigDecimal("100000"), 1));

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(mockOrder));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(mockOrderItems);

        // When
        OrderResponse response = orderService.getOrderByNumber(orderNumber);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.orderNumber()).isEqualTo(orderNumber);

        verify(orderRepository).findByOrderNumber(orderNumber);
        verify(orderItemRepository).findByOrderId(1L);
    }

    @Test
    @DisplayName("주문 취소 성공")
    void 주문취소_성공() {
        // Given
        Long orderId = 1L;
        Order mockOrder = createTestOrder(orderId, "ORD-TEST-001", 1L,
                new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("100000"));

        // Reflection으로 상태를 PENDING으로 설정
        setOrderStatus(mockOrder, Order.OrderStatus.PENDING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        // When
        orderService.cancelOrder(orderId);

        // Then
        assertThat(mockOrder.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);

        verify(orderRepository).findById(orderId);
        verify(orderRepository).save(mockOrder);
    }

    @Test
    @DisplayName("주문 취소 실패 - 존재하지 않는 주문")
    void 주문취소_실패_주문없음() {
        // Given
        Long nonExistentOrderId = 999L;
        when(orderRepository.findById(nonExistentOrderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(nonExistentOrderId))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository).findById(nonExistentOrderId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("주문 상품 정보 검증 - ProductService 연동 없이 미리 조회된 정보 사용")
    void 주문상품정보_검증() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                List.of(new OrderItemRequest(1L, 2)),
                null);

        Map<Long, ProductResponse> productInfoMap = Map.of(
                1L, new ProductResponse(1L, "테스트상품", new BigDecimal("50000"), 10, LocalDateTime.now()));

        Order mockOrder = createTestOrder(1L, "ORD-TEST", 1L,
                new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("100000"));
        OrderItem mockOrderItem = createTestOrderItem(1L, 1L, 1L, "테스트상품", new BigDecimal("50000"), 2);

        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(mockOrderItem);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mock(Payment.class));

        // When
        OrderResponse response = orderService.createOrderWithProductInfo(
                request, new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("100000"), productInfoMap);

        // Then
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productName()).isEqualTo("테스트상품");
        assertThat(response.items().get(0).productPrice()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(response.items().get(0).quantity()).isEqualTo(2);

        // ProductService는 호출되지 않아야 함 (미리 조회된 정보 사용)
        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("주문 생성 시 결제 정보 자동 생성")
    void 주문생성시_결제정보_자동생성() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(1L,
                List.of(new OrderItemRequest(1L, 1)), null);

        Map<Long, ProductResponse> productInfoMap = Map.of(
                1L, new ProductResponse(1L, "상품", new BigDecimal("50000"), 10, LocalDateTime.now()));

        Order mockOrder = createTestOrder(1L, "ORD-TEST", 1L,
                new BigDecimal("50000"), BigDecimal.ZERO, new BigDecimal("50000"));
        OrderItem mockOrderItem = createTestOrderItem(1L, 1L, 1L, "상품", new BigDecimal("50000"), 1);
        Payment mockPayment = createTestPayment(1L, 1L, 1L, new BigDecimal("50000"));

        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(mockOrderItem);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // When
        orderService.createOrderWithProductInfo(
                request, new BigDecimal("50000"), BigDecimal.ZERO, new BigDecimal("50000"), productInfoMap);

        // Then
        verify(paymentRepository).save(any(Payment.class));

        // Payment 객체 생성 검증
        verify(paymentRepository).save(argThat(payment -> payment.getOrderId().equals(1L) &&
                payment.getUserId().equals(1L) &&
                payment.getAmount().compareTo(new BigDecimal("50000")) == 0 &&
                payment.getPaymentMethod() == Payment.PaymentMethod.BALANCE));
    }

    // ==================== 테스트 헬퍼 메서드들 ====================

    /**
     * 테스트용 Order 상태 설정 (Reflection 사용)
     */
    private void setOrderStatus(Order order, Order.OrderStatus status) {
        try {
            var statusField = Order.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(order, status);
        } catch (Exception e) {
            throw new RuntimeException("Order 상태 설정 실패", e);
        }
    }

    /**
     * 테스트용 Order 객체 생성
     */
    private Order createTestOrder(Long id, String orderNumber, Long userId,
            BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal finalAmount) {
        Order order = new Order(orderNumber, userId, totalAmount, discountAmount, finalAmount, null);

        // Reflection으로 ID 설정
        try {
            var idField = Order.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(order, id);

            var createdAtField = Order.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(order, LocalDateTime.now());

            var updatedAtField = Order.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(order, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException("테스트 데이터 생성 실패", e);
        }

        return order;
    }

    /**
     * 테스트용 OrderItem 객체 생성
     */
    private OrderItem createTestOrderItem(Long id, Long orderId, Long productId,
            String productName, BigDecimal productPrice, Integer quantity) {
        OrderItem orderItem = new OrderItem(orderId, productId, productName, productPrice, quantity);

        // Reflection으로 ID 설정
        try {
            var idField = OrderItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(orderItem, id);

            var createdAtField = OrderItem.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(orderItem, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException("테스트 데이터 생성 실패", e);
        }

        return orderItem;
    }

    /**
     * 테스트용 Payment 객체 생성
     */
    private Payment createTestPayment(Long id, Long orderId, Long userId, BigDecimal amount) {
        Payment payment = new Payment(orderId, userId, amount, Payment.PaymentMethod.BALANCE);
        payment.complete(); // 즉시 완료 처리

        // Reflection으로 ID 설정
        try {
            var idField = Payment.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(payment, id);

            var createdAtField = Payment.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(payment, LocalDateTime.now());

            var updatedAtField = Payment.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(payment, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException("테스트 데이터 생성 실패", e);
        }

        return payment;
    }
}