package kr.hhplus.be.server.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.order.dto.OrderItemResponse;
import kr.hhplus.be.server.order.dto.OrderResponse;
import kr.hhplus.be.server.order.service.OrderService;

/**
 * GetOrdersUseCase 단위 테스트
 * 
 * 테스트 전략:
 * - 단일 비즈니스 요구사항 "사용자가 자신의 주문을 조회한다" 검증
 * - OrderService에 올바른 위임 확인
 * - ReadOnly 트랜잭션 적용 확인
 * 
 * 핵심 비즈니스 규칙:
 * - 주문 상세 조회
 * - 사용자별 주문 목록 조회
 * - ReadOnly로 조회만 수행
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetOrdersUseCase 단위 테스트")
class GetOrdersUseCaseTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private GetOrdersUseCase getOrdersUseCase;

    @Test
    @DisplayName("주문 상세 조회 성공 - 주문 ID로 주문을 조회한다")
    void 주문상세조회_성공() {
        // Given
        Long orderId = 1L;
        OrderResponse expectedResponse = createOrderResponse(
                orderId, "ORD-001", 1L,
                new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("100000"));

        when(orderService.getOrder(orderId)).thenReturn(expectedResponse);

        // When
        OrderResponse response = getOrdersUseCase.execute(orderId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.orderNumber()).isEqualTo("ORD-001");
        assertThat(response.finalAmount()).isEqualByComparingTo(new BigDecimal("100000"));

        verify(orderService).getOrder(orderId);
    }

    @Test
    @DisplayName("사용자별 주문 목록 조회 성공 - 사용자 ID로 주문 목록을 조회한다")
    void 사용자별주문목록조회_성공() {
        // Given
        Long userId = 1L;
        List<OrderResponse> expectedResponses = List.of(
                createOrderResponse(1L, "ORD-001", userId,
                        new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("100000")),
                createOrderResponse(2L, "ORD-002", userId,
                        new BigDecimal("50000"), new BigDecimal("5000"), new BigDecimal("45000")));

        when(orderService.getUserOrders(userId)).thenReturn(expectedResponses);

        // When
        List<OrderResponse> responses = getOrdersUseCase.executeUserOrders(userId);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(OrderResponse::orderNumber)
                .containsExactly("ORD-001", "ORD-002");
        assertThat(responses)
                .extracting(OrderResponse::userId)
                .allMatch(id -> id.equals(userId));

        verify(orderService).getUserOrders(userId);
    }

    @Test
    @DisplayName("UseCase 단일 책임 검증 - 조회 관련 메서드만 존재한다")
    void UseCase_단일책임_검증() {
        // Given: UseCase 클래스 메서드 확인
        var methods = GetOrdersUseCase.class.getDeclaredMethods();
        var publicMethods = java.util.Arrays.stream(methods)
                .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
                .filter(method -> !method.getName().equals("equals"))
                .filter(method -> !method.getName().equals("hashCode"))
                .filter(method -> !method.getName().equals("toString"))
                .map(method -> method.getName())
                .sorted()
                .toList();

        // Then: execute와 executeUserOrders 메서드만 존재해야 함
        assertThat(publicMethods).containsExactly("execute", "executeUserOrders");
    }

    @Test
    @DisplayName("ReadOnly 트랜잭션 확인 - @Transactional(readOnly=true)가 적용되어 있다")
    void ReadOnly트랜잭션_확인() {
        // Given: UseCase 클래스
        Class<GetOrdersUseCase> clazz = GetOrdersUseCase.class;

        // Then: @Transactional(readOnly=true) 어노테이션이 적용되어 있어야 함
        var transactional = clazz.getAnnotation(
                org.springframework.transaction.annotation.Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
    }

    @Test
    @DisplayName("UseCase 어노테이션 확인 - @UseCase가 적용되어 있다")
    void UseCase_어노테이션_확인() {
        // Given: UseCase 클래스
        Class<GetOrdersUseCase> clazz = GetOrdersUseCase.class;

        // Then: @UseCase 어노테이션이 적용되어 있어야 함
        boolean hasUseCase = clazz.isAnnotationPresent(
                kr.hhplus.be.server.common.annotation.UseCase.class);

        assertThat(hasUseCase).isTrue();
    }

    @Test
    @DisplayName("의존성 주입 확인 - OrderService만 의존한다")
    void 의존성주입_확인() {
        // Given: UseCase 필드 확인
        var fields = GetOrdersUseCase.class.getDeclaredFields();
        var serviceFields = java.util.Arrays.stream(fields)
                .filter(field -> !field.getName().contains("mockito")) // Mockito 필드 제외
                .filter(field -> !field.getName().contains("Mock")) // Mock 필드 제외
                .filter(field -> !field.getName().equals("log")) // Lombok @Slf4j 필드 제외
                .filter(field -> !field.getType().equals(org.slf4j.Logger.class)) // Logger 타입 제외
                .toList();

        // Then: OrderService 하나만 의존해야 함
        assertThat(serviceFields).hasSize(1);
        assertThat(serviceFields.get(0).getType()).isEqualTo(OrderService.class);
        assertThat(serviceFields.get(0).getName()).isEqualTo("orderService");
    }

    @Test
    @DisplayName("메서드 파라미터 검증 - execute와 executeUserOrders가 올바른 파라미터를 받는다")
    void 메서드파라미터_검증() throws NoSuchMethodException {
        // Given: 메서드 시그니처 확인
        var executeMethod = GetOrdersUseCase.class.getDeclaredMethod("execute", Long.class);
        var userOrdersMethod = GetOrdersUseCase.class.getDeclaredMethod("executeUserOrders", Long.class);

        // Then: 메서드 시그니처가 올바른지 확인
        assertThat(executeMethod.getReturnType()).isEqualTo(OrderResponse.class);
        assertThat(userOrdersMethod.getReturnType()).isEqualTo(List.class);

        // 파라미터 타입 확인
        assertThat(executeMethod.getParameterTypes()).containsExactly(Long.class);
        assertThat(userOrdersMethod.getParameterTypes()).containsExactly(Long.class);
    }

    // ==================== 테스트 헬퍼 메서드 ====================

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
                List.of(new OrderItemResponse(1L, "테스트상품", new BigDecimal("50000"), 2, new BigDecimal("100000"))));
    }
}