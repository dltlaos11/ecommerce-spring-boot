package kr.hhplus.be.server.order.infrastructure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.hhplus.be.server.order.domain.OrderItem;

/**
 * OrderItem JPA Repository (Infrastructure Layer)
 */
public interface OrderItemJpaRepository extends JpaRepository<OrderItem, Long> {

    /**
     * 주문별 항목 목록 조회
     * Repository 2번 호출
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * 상품별 주문 항목 조회 (통계용)
     */
    List<OrderItem> findByProductId(Long productId);

    /**
     * 주문 ID로 모든 항목 삭제
     */
    void deleteByOrderId(Long orderId);

    /**
     * 복잡한 쿼리 (이런 것만 Repository 테스트 작성)
     * 인기 상품 통계용 - 주문 ID 목록으로 조회
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.orderId IN :orderIds ORDER BY oi.orderId, oi.id")
    List<OrderItem> findByOrderIdIn(@Param("orderIds") List<Long> orderIds);
}
