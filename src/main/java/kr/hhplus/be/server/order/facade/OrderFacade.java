// OrderFacade.java
package kr.hhplus.be.server.order.facade;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.balance.service.BalanceService;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.coupon.service.CouponService;
import kr.hhplus.be.server.order.dto.CreateOrderRequest;
import kr.hhplus.be.server.order.dto.OrderItemRequest;
import kr.hhplus.be.server.order.dto.OrderResponse;
import kr.hhplus.be.server.order.service.OrderService;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 Facade - 복합 도메인 조정자
 * 
 * Facade 패턴 적용:
 * - 여러 도메인 서비스들을 조합하여 복잡한 비즈니스 워크플로우 처리
 * - 단일 진입점으로 클라이언트 코드 단순화
 * - 도메인 간 의존성을 Facade에서 관리
 * 
 * 책임:
 * - 주문 생성 워크플로우 조정 (재고확인 → 쿠폰적용 → 잔액결제 → 주문생성)
 * - 트랜잭션 경계 관리
 * - 도메인 서비스 간 데이터 변환
 * 
 * 주문 생성 플로우:
 * 1. 상품 재고 검증
 * 2. 쿠폰 할인 계산 (선택사항)
 * 3. 최종 금액 계산
 * 4. 잔액 차감
 * 5. 재고 차감
 * 6. 쿠폰 사용 처리
 * 7. 주문 생성
 */
@Slf4j
@Component
@Transactional
public class OrderFacade {

    private final OrderService orderService;
    private final ProductService productService;
    private final BalanceService balanceService;
    private final CouponService couponService;

    /**
     * 생성자 주입 - 모든 도메인 서비스 의존성 주입
     */
    public OrderFacade(OrderService orderService,
            ProductService productService,
            BalanceService balanceService,
            CouponService couponService) {
        this.orderService = orderService;
        this.productService = productService;
        this.balanceService = balanceService;
        this.couponService = couponService;
    }

    /**
     * 주문 생성 - 메인 비즈니스 워크플로우
     * 
     * 처리 순서:
     * 1. 재고 검증 → 2. 쿠폰 할인 계산 → 3. 잔액 결제 → 4. 재고 차감 → 5. 쿠폰 사용 → 6. 주문 생성
     * 
     * @param request 주문 생성 요청
     * @return 생성된 주문 정보
     */
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("🛒 주문 생성 워크플로우 시작: userId = {}, 상품 {}개, 쿠폰 = {}",
                request.userId(), request.items().size(), request.couponId());

        try {
            // 1️⃣ 재고 검증 - 모든 상품의 재고가 충분한지 확인
            validateProductStock(request.items());
            log.debug("✅ 1단계: 재고 검증 완료");

            // 2️⃣ 총 주문 금액 계산
            BigDecimal totalAmount = calculateTotalAmount(request.items());
            log.debug("💰 총 주문 금액: {}", totalAmount);

            // 3️⃣ 쿠폰 할인 적용 (선택사항)
            BigDecimal discountAmount = BigDecimal.ZERO;
            if (request.couponId() != null) {
                discountAmount = applyCouponDiscount(request.userId(), request.couponId(), totalAmount);
                log.debug("🎫 쿠폰 할인 금액: {}", discountAmount);
            }

            // 4️⃣ 최종 결제 금액 계산
            BigDecimal finalAmount = totalAmount.subtract(discountAmount);
            log.debug("💳 최종 결제 금액: {}", finalAmount);

            // 5️⃣ 잔액 결제 처리
            processPayment(request.userId(), finalAmount);
            log.debug("✅ 2단계: 잔액 결제 완료");

            // 6️⃣ 재고 차감 처리
            deductProductStock(request.items());
            log.debug("✅ 3단계: 재고 차감 완료");

            // 7️⃣ 쿠폰 사용 처리 (있는 경우)
            if (request.couponId() != null) {
                processCouponUsage(request.userId(), request.couponId(), totalAmount);
                log.debug("✅ 4단계: 쿠폰 사용 처리 완료");
            }

            // 8️⃣ 주문 생성 (상품 정보 포함)
            Map<Long, ProductResponse> productInfoMap = getProductInfoMap(request.items());
            OrderResponse orderResponse = orderService.createOrderWithProductInfo(
                    request, totalAmount, discountAmount, finalAmount, productInfoMap);

            log.info("🎉 주문 생성 워크플로우 완료: 주문번호 = {}, 최종금액 = {}",
                    orderResponse.orderNumber(), finalAmount);

            return orderResponse;

        } catch (Exception e) {
            log.error("❌ 주문 생성 실패: userId = {}, 에러 = {}", request.userId(), e.getMessage());
            throw e;
        }
    }

    /**
     * 주문 상세 조회 (위임)
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        log.debug("🔍 주문 조회: orderId = {}", orderId);
        return orderService.getOrder(orderId);
    }

    /**
     * 사용자별 주문 목록 조회 (위임)
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(Long userId) {
        log.debug("📋 사용자 주문 목록 조회: userId = {}", userId);
        return orderService.getUserOrders(userId);
    }

    // ==================== 내부 헬퍼 메서드들 ====================

    /**
     * 1단계: 재고 검증 - 모든 상품의 재고가 충분한지 확인
     */
    private void validateProductStock(List<OrderItemRequest> items) {
        for (OrderItemRequest item : items) {
            ProductResponse product = productService.getProduct(item.productId());

            if (!productService.hasEnoughStock(item.productId(), item.quantity())) {
                throw new kr.hhplus.be.server.product.exception.InsufficientStockException(
                        ErrorCode.INSUFFICIENT_STOCK,
                        String.format("상품 '%s'의 재고가 부족합니다. 요청: %d, 재고: %d",
                                product.name(), item.quantity(), product.stockQuantity()));
            }
        }
    }

    /**
     * 2단계: 총 주문 금액 계산
     */
    private BigDecimal calculateTotalAmount(List<OrderItemRequest> items) {
        return items.stream()
                .map(item -> {
                    ProductResponse product = productService.getProduct(item.productId());
                    return product.price().multiply(new BigDecimal(item.quantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 3단계: 쿠폰 할인 적용
     */
    private BigDecimal applyCouponDiscount(Long userId, Long couponId, BigDecimal totalAmount) {
        // 쿠폰 검증 및 할인 금액 계산
        var validation = couponService.validateAndCalculateDiscount(userId, couponId, totalAmount);

        if (!validation.usable()) {
            throw new IllegalArgumentException("쿠폰을 사용할 수 없습니다: " + validation.reason());
        }

        return validation.discountAmount();
    }

    /**
     * 4단계: 잔액 결제 처리
     */
    private void processPayment(Long userId, BigDecimal amount) {
        // 잔액 충분 여부 확인
        if (!balanceService.hasEnoughBalance(userId, amount)) {
            throw new kr.hhplus.be.server.balance.exception.InsufficientBalanceException(
                    ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 잔액 차감 (주문 ID는 아직 생성되지 않아서 임시 ID 사용)
        String tempOrderId = "TEMP_" + userId + "_" + System.currentTimeMillis();
        balanceService.deductBalance(userId, amount, tempOrderId);
    }

    /**
     * 5단계: 재고 차감 처리
     */
    private void deductProductStock(List<OrderItemRequest> items) {
        for (OrderItemRequest item : items) {
            productService.reduceStock(item.productId(), item.quantity());
        }
    }

    /**
     * 6단계: 쿠폰 사용 처리
     */
    private void processCouponUsage(Long userId, Long couponId, BigDecimal totalAmount) {
        couponService.useCoupon(userId, couponId, totalAmount);
    }

    /**
     * 상품 정보 맵 생성 (Facade에서 미리 조회해서 Service에 전달)
     */
    private Map<Long, ProductResponse> getProductInfoMap(List<OrderItemRequest> items) {
        return items.stream()
                .collect(Collectors.toMap(
                        OrderItemRequest::productId,
                        item -> productService.getProduct(item.productId()),
                        (existing, replacement) -> existing // 중복 시 기존 값 유지
                ));
    }
}