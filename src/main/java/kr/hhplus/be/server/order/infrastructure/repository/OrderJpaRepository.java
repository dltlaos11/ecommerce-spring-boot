package kr.hhplus.be.server.order.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.hhplus.be.server.order.domain.Order;

/**
 * Order JPA Repository (Infrastructure Layer)
 */
public interface OrderJpaRepository extends JpaRepository<Order, Long> {

    /**
     * 주문 번호로 주문 조회
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * 사용자별 주문 목록 조회 (최신순)
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 상태의 주문 목록 조회
     */
    List<Order> findByStatus(Order.OrderStatus status);

    /**
     * 사용자별 특정 상태 주문 조회
     */
    List<Order> findByUserIdAndStatus(Long userId, Order.OrderStatus status);
}