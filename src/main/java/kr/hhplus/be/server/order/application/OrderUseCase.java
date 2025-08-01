package kr.hhplus.be.server.order.application;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.common.annotation.UseCase;
import kr.hhplus.be.server.order.dto.CreateOrderRequest;
import kr.hhplus.be.server.order.dto.OrderResponse;
import kr.hhplus.be.server.order.facade.OrderFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Application Layer - 주문 비즈니스 Usecase
 * 
 * 책임:
 * - 여러 Domain Service를 조합하여 비즈니스 유스케이스 구현
 * - 트랜잭션 경계 관리
 * - Controller와 Domain Layer 사이의 조정
 * 
 * - 단위 기능 기반의 Application Layer (응용 계층 - 비즈니스 Usecase)
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class OrderUseCase {

    private final OrderFacade orderFacade; // 복합 도메인 조정자 활용

    /**
     * 주문 생성 유스케이스
     * 
     * 비즈니스 플로우:
     * 1. 재고 검증 → 2. 쿠폰 할인 계산 → 3. 잔액 결제 → 4. 재고 차감 → 5. 쿠폰 사용 → 6. 주문 생성
     */
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("📝 주문 생성 유스케이스 실행: userId = {}, 상품 {}개",
                request.userId(), request.items().size());

        // OrderFacade에 위임 (복합 도메인 워크플로우)
        OrderResponse response = orderFacade.createOrder(request);

        log.info("✅ 주문 생성 유스케이스 완료: orderNumber = {}", response.orderNumber());
        return response;
    }

    /**
     * 주문 조회 유스케이스
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        log.debug("🔍 주문 조회 유스케이스: orderId = {}", orderId);
        return orderFacade.getOrder(orderId);
    }

    /**
     * 사용자별 주문 목록 조회 유스케이스
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(Long userId) {
        log.debug("📋 사용자 주문 목록 조회 유스케이스: userId = {}", userId);
        return orderFacade.getUserOrders(userId);
    }
}