package kr.hhplus.be.server.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.Payment;
import kr.hhplus.be.server.order.dto.CreateOrderRequest;
import kr.hhplus.be.server.order.dto.OrderItemRequest;
import kr.hhplus.be.server.order.dto.OrderItemResponse;
import kr.hhplus.be.server.order.dto.OrderResponse;
import kr.hhplus.be.server.order.exception.OrderNotFoundException;
import kr.hhplus.be.server.order.repository.OrderItemRepository;
import kr.hhplus.be.server.order.repository.OrderRepository;
import kr.hhplus.be.server.order.repository.PaymentRepository;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 서비스
 * 
 * 설계 원칙:
 * - 단일 책임: 주문 관련 기본 CRUD만 처리
 * - 복합 비즈니스 로직은 OrderFacade에서 처리
 * - 의존성 역전: Repository 인터페이스에만 의존
 * 
 * 수정사항:
 * - ProductService 의존성 추가로 실제 상품 정보 조회
 * - Mock 데이터 제거하고 실제 데이터 사용
 * - 주문 항목 생성 시 실제 상품 정보 활용
 * 
 * 책임:
 * - 주문 생성/조회/상태 변경
 * - 주문 항목 관리 (실제 상품 정보 연동)
 * - 결제 정보 관리
 * - DTO 변환
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ProductService productService; // 🆕 ProductService 추가

    public OrderService(OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            ProductService productService) { // 🆕 생성자에 ProductService 추가
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.productService = productService;
    }

    /**
     * 주문 생성 (상품 정보 포함) - Facade에서 호출
     * 
     * @param request        주문 생성 요청
     * @param totalAmount    총 주문 금액
     * @param discountAmount 할인 금액
     * @param finalAmount    최종 결제 금액
     * @param productInfoMap 미리 조회된 상품 정보 맵
     * @return 생성된 주문 정보
     */
    @Transactional
    public OrderResponse createOrderWithProductInfo(CreateOrderRequest request, BigDecimal totalAmount,
            BigDecimal discountAmount, BigDecimal finalAmount, java.util.Map<Long, ProductResponse> productInfoMap) {
        log.info("📝 주문 생성 처리 (상품정보포함): userId = {}, 총액 = {}, 최종액 = {}",
                request.userId(), totalAmount, finalAmount);

        // 1. 주문 번호 생성
        String orderNumber = generateOrderNumber();

        // 2. 주문 생성
        Order order = new Order(orderNumber, request.userId(), totalAmount,
                discountAmount, finalAmount, request.couponId());
        Order savedOrder = orderRepository.save(order);

        // 3. 주문 항목들 생성 (미리 조회된 상품 정보 사용)
        List<OrderItem> orderItems = createOrderItemsWithProductInfo(savedOrder, request.items(), productInfoMap);

        // 4. 결제 정보 생성
        Payment payment = new Payment(savedOrder.getId(), request.userId(),
                finalAmount, Payment.PaymentMethod.BALANCE);
        payment.complete(); // 잔액 결제는 즉시 완료
        paymentRepository.save(payment);

        // 5. 주문 완료 처리
        savedOrder.complete();
        orderRepository.save(savedOrder);

        log.info("✅ 주문 생성 완료: 주문번호 = {}, ID = {}", orderNumber, savedOrder.getId());

        return convertToOrderResponse(savedOrder, orderItems);
    }

    /**
     * 주문 상세 조회
     */
    public OrderResponse getOrder(Long orderId) {
        log.debug("🔍 주문 조회 요청: orderId = {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("❌ 주문을 찾을 수 없음: orderId = {}", orderId);
                    return new OrderNotFoundException(ErrorCode.ORDER_NOT_FOUND);
                });

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        log.debug("✅ 주문 조회 완료: 주문번호 = {}", order.getOrderNumber());

        return convertToOrderResponse(order, orderItems);
    }

    /**
     * 사용자별 주문 목록 조회
     */
    public List<OrderResponse> getUserOrders(Long userId) {
        log.debug("📋 사용자 주문 목록 조회: userId = {}", userId);

        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);

        log.debug("✅ 사용자 주문 조회 완료: {}개", orders.size());

        return orders.stream()
                .map(order -> {
                    List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
                    return convertToOrderResponse(order, orderItems);
                })
                .toList();
    }

    /**
     * 주문 번호로 조회
     */
    public OrderResponse getOrderByNumber(String orderNumber) {
        log.debug("🔍 주문번호로 조회: orderNumber = {}", orderNumber);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(ErrorCode.ORDER_NOT_FOUND));

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

        return convertToOrderResponse(order, orderItems);
    }

    /**
     * 주문 취소
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        log.info("❌ 주문 취소 요청: orderId = {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(ErrorCode.ORDER_NOT_FOUND));

        order.cancel();
        orderRepository.save(order);

        log.info("✅ 주문 취소 완료: 주문번호 = {}", order.getOrderNumber());
    }

    /**
     * 주문 항목들 생성 (미리 조회된 상품 정보 사용)
     */
    private List<OrderItem> createOrderItemsWithProductInfo(Order order, List<OrderItemRequest> itemRequests,
            java.util.Map<Long, ProductResponse> productInfoMap) {
        return itemRequests.stream()
                .map(itemRequest -> {
                    // 미리 조회된 상품 정보 사용
                    ProductResponse product = productInfoMap.get(itemRequest.productId());
                    if (product == null) {
                        throw new IllegalArgumentException("상품 정보를 찾을 수 없습니다: " + itemRequest.productId());
                    }

                    OrderItem orderItem = new OrderItem(
                            order.getId(),
                            itemRequest.productId(),
                            product.name(),
                            product.price(),
                            itemRequest.quantity());

                    // orderItem.setOrder(order);
                    return orderItemRepository.save(orderItem);
                })
                .toList();
    }

    /**
     * 주문 항목들 생성 (실제 상품 정보 조회) - 기존 메서드 유지
     */
    private List<OrderItem> createOrderItems(Order order, List<OrderItemRequest> itemRequests) {
        return itemRequests.stream()
                .map(itemRequest -> {
                    // 🆕 실제 ProductService에서 상품 정보 조회
                    ProductResponse product = productService.getProduct(itemRequest.productId());

                    OrderItem orderItem = new OrderItem(
                            order.getId(),
                            itemRequest.productId(),
                            product.name(),
                            product.price(),
                            itemRequest.quantity());

                    // orderItem.setOrder(order);
                    return orderItemRepository.save(orderItem);
                })
                .toList();
    }

    /**
     * 주문 번호 생성
     */
    private String generateOrderNumber() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("ORD-%s-%s", datePrefix, randomSuffix);
    }

    /**
     * Order와 OrderItem을 OrderResponse DTO로 변환
     */
    private OrderResponse convertToOrderResponse(Order order, List<OrderItem> orderItems) {
        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getProductPrice(),
                        item.getQuantity(),
                        item.getSubtotal()))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getStatus().getCode(),
                order.getCreatedAt(),
                itemResponses);
    }
}