package kr.hhplus.be.server.order.application;

import java.util.List;

import kr.hhplus.be.server.common.annotation.UseCase;
import kr.hhplus.be.server.order.dto.OrderResponse;
import kr.hhplus.be.server.order.service.OrderService;
import lombok.RequiredArgsConstructor;

/**
 * 주문 조회 UseCase - OrderFacade 제거하고 직접 OrderService 호출
 * 
 * 구체적인 요구사항: "사용자가 자신의 주문을 조회한다"
 * 
 * 멘토님 피드백 반영:
 * - 파사드 패턴 제거 (단순한 조회는 직접 서비스 호출)
 * - UseCase가 실제 비즈니스 로직 수행
 */
@UseCase
@RequiredArgsConstructor
public class GetOrdersUseCase {

    private final OrderService orderService;

    /**
     * 주문 상세 조회
     */
    public OrderResponse execute(Long orderId) {
        return orderService.getOrder(orderId);
    }

    /**
     * 사용자별 주문 목록 조회
     */
    public List<OrderResponse> executeUserOrders(Long userId) {
        return orderService.getUserOrders(userId);
    }
}