package kr.hhplus.be.server.order.repository;

import java.util.List;
import java.util.Optional;

import kr.hhplus.be.server.order.domain.OrderItem;

/**
 * 주문 항목 저장소 인터페이스 (Domain Layer)
 * N+1 문제 해결을 위한 배치 조회 메서드 추가
 */
public interface OrderItemRepository {

    Optional<OrderItem> findById(Long id);

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByProductId(Long productId);

    OrderItem save(OrderItem orderItem);

    void delete(OrderItem orderItem);

    void deleteById(Long id);

    void deleteByOrderId(Long orderId);

    List<OrderItem> findAll();

    /**
     * N+1 문제 해결: 여러 주문의 항목들을 한 번에 조회
     * 
     * @param orderIds 주문 ID 목록
     * @return 해당 주문들의 모든 항목
     */
    List<OrderItem> findByOrderIdIn(List<Long> orderIds);
}