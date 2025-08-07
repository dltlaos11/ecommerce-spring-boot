package kr.hhplus.be.server.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
import kr.hhplus.be.server.order.dto.CreateOrderRequest;
import kr.hhplus.be.server.order.dto.OrderItemRequest;
import kr.hhplus.be.server.order.dto.OrderResponse;
import kr.hhplus.be.server.order.service.OrderService;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.product.exception.InsufficientStockException;
import kr.hhplus.be.server.product.service.ProductService;

/**
 * CreateOrderUseCase 단위 테스트
 * 
 * 테스트 전략:
 * - 복잡한 주문 워크플로우의 각 단계별 검증
 * - 외부 서비스들과의 상호작용 검증
 * - 예외 상황별 시나리오 테스트
 * - 트랜잭션 경계 및 롤백 시나리오
 * 
 * 핵심 비즈니스 워크플로우:
 * 1. 재고 검증 → 2. 쿠폰 할인 계산 → 3. 잔액 결제 → 4. 재고 차감 → 5. 쿠폰 사용 → 6. 주문 생성
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOrderUseCase 단위 테스트")
class CreateOrderUseCaseTest {

    @Mock
    private OrderService orderService;

    @Mock
    private ProductService productService;

    @Mock
    private BalanceService balanceService;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private CreateOrderUseCase createOrderUseCase;

    @Test
    @DisplayName("주문 생성 성공 - 쿠폰 없는 일반 주문")
    void 주문생성_성공_일반주문() {
        // Given
        Long userId = 1L;
        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                List.of(new OrderItemRequest(1L, 2)),
                null // 쿠폰 없음
        );

        // 상품 조회 Mock
        ProductResponse product = new ProductResponse(1L, "테스트 노트북",
                new BigDecimal("50000"), 10, LocalDateTime.now());
        when(productService.getProduct(1L)).thenReturn(product);
        when(productService.hasEnoughStock(1L, 2)).thenReturn(true);

        // 잔액 검증 Mock
        when(balanceService.hasEnoughBalance(userId, new BigDecimal("100000"))).thenReturn(true);

        // 주문 서비스 Mock
        OrderResponse expectedResponse = createOrderResponse(1L, "ORD-001", userId,
                new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("100000"));
        when(orderService.createOrderWithProductInfo(
                eq(request),
                eq(new BigDecimal("100000")),
                eq(BigDecimal.ZERO),
                eq(new BigDecimal("100000")),
                any(Map.class))).thenReturn(expectedResponse);

        // When
        OrderResponse response = createOrderUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(response.finalAmount()).isEqualByComparingTo(new BigDecimal("100000"));

        // 워크플로우 검증
        verify(productService).getProduct(1L);
        verify(productService).hasEnoughStock(1L, 2);
        verify(balanceService).hasEnoughBalance(userId, new BigDecimal("100000"));
        verify(balanceService).deductBalance(eq(userId), eq(new BigDecimal("100000")), anyString());
        verify(productService).reduceStock(1L, 2);
        verify(orderService).createOrderWithProductInfo(any(), any(), any(), any(), any());

        // 쿠폰 서비스는 호출되지 않아야 함
        verifyNoInteractions(couponService);
    }

    @Test
    @DisplayName("주문 생성 성공 - 쿠폰 적용 주문")
    void 주문생성_성공_쿠폰적용() {
        // Given
        Long userId = 1L;
        Long couponId = 10L;
        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                List.of(new OrderItemRequest(1L, 2)),
                couponId);

        // 상품 조회 Mock
        ProductResponse product = new ProductResponse(1L, "테스트 상품",
                new BigDecimal("75000"), 5, LocalDateTime.now());
        when(productService.getProduct(1L)).thenReturn(product);
        when(productService.hasEnoughStock(1L, 2)).thenReturn(true);

        // 쿠폰 검증 및 할인 Mock
        CouponValidationResponse couponValidation = new CouponValidationResponse(
                couponId, userId, true, new BigDecimal("15000"), new BigDecimal("135000"), null);
        when(couponService.validateAndCalculateDiscount(userId, couponId, new BigDecimal("150000")))
                .thenReturn(couponValidation);

        // 잔액 검증 Mock (할인된 금액으로)
        when(balanceService.hasEnoughBalance(userId, new BigDecimal("135000"))).thenReturn(true);

        // 주문 서비스 Mock
        OrderResponse expectedResponse = createOrderResponse(1L, "ORD-002", userId,
                new BigDecimal("150000"), new BigDecimal("15000"), new BigDecimal("135000"));
        when(orderService.createOrderWithProductInfo(
                eq(request),
                eq(new BigDecimal("150000")),
                eq(new BigDecimal("15000")),
                eq(new BigDecimal("135000")),
                any(Map.class))).thenReturn(expectedResponse);

        // When
        OrderResponse response = createOrderUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("150000"));
        assertThat(response.discountAmount()).isEqualByComparingTo(new BigDecimal("15000"));
        assertThat(response.finalAmount()).isEqualByComparingTo(new BigDecimal("135000"));

        // 쿠폰 워크플로우 검증
        verify(couponService).validateAndCalculateDiscount(userId, couponId, new BigDecimal("150000"));
        verify(balanceService).deductBalance(eq(userId), eq(new BigDecimal("135000")), anyString());
        verify(couponService).useCoupon(userId, couponId, new BigDecimal("150000"));
        verify(orderService).createOrderWithProductInfo(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("주문 생성 실패 - 재고 부족")
    void 주문생성_실패_재고부족() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                List.of(new OrderItemRequest(1L, 5)), // 5개 주문
                null);

        ProductResponse product = new ProductResponse(1L, "재고부족상품",
                new BigDecimal("50000"), 3, LocalDateTime.now()); // 재고 3개만
        when(productService.getProduct(1L)).thenReturn(product);
        when(productService.hasEnoughStock(1L, 5)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.execute(request))
                .isInstanceOf(InsufficientStockException.class);

        // 재고 검증만 호출되고 나머지는 호출되지 않아야 함
        verify(productService).getProduct(1L);
        verify(productService).hasEnoughStock(1L, 5);
        verifyNoInteractions(balanceService, couponService, orderService);
    }

    @Test
    @DisplayName("주문 생성 실패 - 잔액 부족")
    void 주문생성_실패_잔액부족() {
        // Given
        Long userId = 1L;
        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                List.of(new OrderItemRequest(1L, 2)),
                null);

        // 재고는 충분하지만 잔액이 부족한 상황
        ProductResponse product = new ProductResponse(1L, "고가상품",
                new BigDecimal("100000"), 10, LocalDateTime.now());
        when(productService.getProduct(1L)).thenReturn(product);
        when(productService.hasEnoughStock(1L, 2)).thenReturn(true);
        when(balanceService.hasEnoughBalance(userId, new BigDecimal("200000"))).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.execute(request))
                .isInstanceOf(InsufficientBalanceException.class);

        // 잔액 검증까지만 호출되고 실제 차감은 호출되지 않아야 함
        verify(productService).getProduct(1L);
        verify(productService).hasEnoughStock(1L, 2);
        verify(balanceService).hasEnoughBalance(userId, new BigDecimal("200000"));
        verify(balanceService, never()).deductBalance(any(), any(), any());
        verify(productService, never()).reduceStock(any(), any());
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("주문 생성 실패 - 쿠폰 사용 불가")
    void 주문생성_실패_쿠폰사용불가() {
        // Given
        Long userId = 1L;
        Long couponId = 10L;
        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                List.of(new OrderItemRequest(1L, 1)),
                couponId);

        ProductResponse product = new ProductResponse(1L, "상품",
                new BigDecimal("50000"), 10, LocalDateTime.now());
        when(productService.getProduct(1L)).thenReturn(product);
        when(productService.hasEnoughStock(1L, 1)).thenReturn(true);

        // 쿠폰 검증 실패
        CouponValidationResponse couponValidation = new CouponValidationResponse(
                couponId, userId, false, BigDecimal.ZERO, new BigDecimal("50000"),
                "최소 주문 금액을 만족하지 않습니다");
        when(couponService.validateAndCalculateDiscount(userId, couponId, new BigDecimal("50000")))
                .thenReturn(couponValidation);

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("쿠폰을 사용할 수 없습니다: 최소 주문 금액을 만족하지 않습니다");

        // 쿠폰 검증까지만 호출되고 나머지는 호출되지 않아야 함
        verify(productService).getProduct(1L);
        verify(productService).hasEnoughStock(1L, 1);
        verify(couponService).validateAndCalculateDiscount(userId, couponId, new BigDecimal("50000"));
        verifyNoInteractions(balanceService, orderService);
    }

    @Test
    @DisplayName("주문 생성 성공 - 여러 상품 주문")
    void 주문생성_성공_여러상품() {
        // Given
        Long userId = 1L;
        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                List.of(
                        new OrderItemRequest(1L, 1), // 50000원 상품 1개
                        new OrderItemRequest(2L, 2) // 30000원 상품 2개
                ),
                null);

        // 상품 Mock 설정
        ProductResponse product1 = new ProductResponse(1L, "상품1",
                new BigDecimal("50000"), 10, LocalDateTime.now());
        ProductResponse product2 = new ProductResponse(2L, "상품2",
                new BigDecimal("30000"), 5, LocalDateTime.now());

        when(productService.getProduct(1L)).thenReturn(product1);
        when(productService.getProduct(2L)).thenReturn(product2);
        when(productService.hasEnoughStock(1L, 1)).thenReturn(true);
        when(productService.hasEnoughStock(2L, 2)).thenReturn(true);

        // 총 금액: 50000 + (30000 * 2) = 110000
        when(balanceService.hasEnoughBalance(userId, new BigDecimal("110000"))).thenReturn(true);

        OrderResponse expectedResponse = createOrderResponse(1L, "ORD-003", userId,
                new BigDecimal("110000"), BigDecimal.ZERO, new BigDecimal("110000"));
        when(orderService.createOrderWithProductInfo(any(), any(), any(), any(), any()))
                .thenReturn(expectedResponse);

        // When
        OrderResponse response = createOrderUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.finalAmount()).isEqualByComparingTo(new BigDecimal("110000"));

        // 모든 상품에 대해 검증되어야 함
        verify(productService).getProduct(1L);
        verify(productService).getProduct(2L);
        verify(productService).hasEnoughStock(1L, 1);
        verify(productService).hasEnoughStock(2L, 2);
        verify(productService).reduceStock(1L, 1);
        verify(productService).reduceStock(2L, 2);
        verify(balanceService).deductBalance(eq(userId), eq(new BigDecimal("110000")), anyString());
    }

    @Test
    @DisplayName("UseCase 단일 책임 검증 - execute 메서드만 존재한다")
    void UseCase_단일책임_검증() {
        // Given: UseCase 클래스 메서드 확인
        var methods = CreateOrderUseCase.class.getDeclaredMethods();
        var publicMethods = java.util.Arrays.stream(methods)
                .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
                .filter(method -> !method.getName().equals("equals"))
                .filter(method -> !method.getName().equals("hashCode"))
                .filter(method -> !method.getName().equals("toString"))
                .toList();

        // Then: execute 메서드만 존재해야 함 (단일 책임)
        assertThat(publicMethods).hasSize(1);
        assertThat(publicMethods.get(0).getName()).isEqualTo("execute");
    }

    @Test
    @DisplayName("트랜잭션 어노테이션 확인 - @Transactional이 적용되어 있다")
    void 트랜잭션_어노테이션_확인() {
        // Given: UseCase 클래스
        Class<CreateOrderUseCase> clazz = CreateOrderUseCase.class;

        // Then: @Transactional 어노테이션이 적용되어 있어야 함
        boolean hasTransactional = clazz.isAnnotationPresent(
                org.springframework.transaction.annotation.Transactional.class);

        assertThat(hasTransactional).isTrue();
    }

    @Test
    @DisplayName("UseCase 어노테이션 확인 - @UseCase가 적용되어 있다")
    void UseCase_어노테이션_확인() {
        // Given: UseCase 클래스
        Class<CreateOrderUseCase> clazz = CreateOrderUseCase.class;

        // Then: @UseCase 어노테이션이 적용되어 있어야 함
        boolean hasUseCase = clazz.isAnnotationPresent(
                kr.hhplus.be.server.common.annotation.UseCase.class);

        assertThat(hasUseCase).isTrue();
    }

    @Test
    @DisplayName("의존성 주입 확인 - 필요한 서비스들만 의존한다")
    void 의존성주입_확인() {
        // Given: UseCase 필드 확인
        var fields = CreateOrderUseCase.class.getDeclaredFields();
        var serviceFields = java.util.Arrays.stream(fields)
                .filter(field -> !field.getName().contains("mockito")) // Mockito 필드 제외
                .filter(field -> !field.getName().contains("Mock")) // Mock 필드 제외
                .filter(field -> !field.getName().equals("log")) // Lombok @Slf4j 필드 제외
                .filter(field -> !field.getType().equals(org.slf4j.Logger.class)) // Logger 타입 제외
                .toList();

        // Then: 필요한 서비스들만 의존해야 함
        assertThat(serviceFields).hasSize(4); // OrderService, ProductService, BalanceService, CouponService

        var fieldNames = serviceFields.stream().map(field -> field.getName()).toList();
        assertThat(fieldNames).containsExactlyInAnyOrder(
                "orderService", "productService", "balanceService", "couponService");
    }

    @Test
    @DisplayName("워크플로우 순서 검증 - 비즈니스 로직이 올바른 순서로 실행된다")
    void 워크플로우순서_검증() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(1L,
                List.of(new OrderItemRequest(1L, 1)), null);

        ProductResponse product = new ProductResponse(1L, "상품",
                new BigDecimal("50000"), 10, LocalDateTime.now());
        when(productService.getProduct(1L)).thenReturn(product);
        when(productService.hasEnoughStock(1L, 1)).thenReturn(true);
        when(balanceService.hasEnoughBalance(1L, new BigDecimal("50000"))).thenReturn(true);

        OrderResponse expectedResponse = createOrderResponse(1L, "ORD-004", 1L,
                new BigDecimal("50000"), BigDecimal.ZERO, new BigDecimal("50000"));
        when(orderService.createOrderWithProductInfo(any(), any(), any(), any(), any()))
                .thenReturn(expectedResponse);

        // When
        createOrderUseCase.execute(request);

        // Then: 호출 순서 검증
        var inOrder = inOrder(productService, balanceService, orderService);

        // 1. 재고 검증
        inOrder.verify(productService).getProduct(1L);
        inOrder.verify(productService).hasEnoughStock(1L, 1);

        // 2. 잔액 검증 및 차감
        inOrder.verify(balanceService).hasEnoughBalance(1L, new BigDecimal("50000"));
        inOrder.verify(balanceService).deductBalance(eq(1L), eq(new BigDecimal("50000")), anyString());

        // 3. 재고 차감
        inOrder.verify(productService).reduceStock(1L, 1);

        // 4. 주문 생성
        inOrder.verify(orderService).createOrderWithProductInfo(any(), any(), any(), any(), any());
    }

    // ==================== 테스트 헬퍼 메서드들 ====================

    /**
     * 테스트용 OrderResponse 생성
     */
    private OrderResponse createOrderResponse(Long orderId, String orderNumber, Long userId,
            BigDecimal totalAmount, BigDecimal discountAmount,
            BigDecimal finalAmount) {
        return new OrderResponse(
                orderId,
                orderNumber,
                userId,
                totalAmount,
                discountAmount,
                finalAmount,
                "COMPLETED",
                LocalDateTime.now(),
                List.of() // 빈 주문 항목 (여기서는 중요하지 않음)
        );
    }
}