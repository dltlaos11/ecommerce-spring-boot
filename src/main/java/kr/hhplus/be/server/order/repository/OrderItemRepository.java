package kr.hhplus.be.server.order.repository;

import java.util.List;
import java.util.Optional;

import kr.hhplus.be.server.order.domain.OrderItem;

/**
 * 주문 항목 저장소 인터페이스 (Domain Layer)
 * 
 * 설계 원칙:
 * - DIP(의존성 역전 원칙) 적용
 * - 주문별 항목 관리 지원
 * - 상품별 주문 통계 지원
 * 
 * 책임:
 * - 주문 항목 CRUD
 * - 주문별 항목 목록 조회
 * - 상품별 판매 통계 지원
 */
public interface OrderItemRepository {

    /**
     * ID로 주문 항목 조회
     * 
     * @param id 주문 항목 ID
     * @return 주문 항목 정보
     */
    Optional<OrderItem> findById(Long id);

    /**
     * 주문별 항목 목록 조회
     * 
     * @param orderId 주문 ID
     * @return 해당 주문의 항목 목록
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * 상품별 주문 항목 조회 (통계용)
     * 
     * @param productId 상품 ID
     * @return 해당 상품의 주문 항목 목록
     */
    List<OrderItem> findByProductId(Long productId);

    /**
     * 주문 항목 저장 (생성 또는 수정)
     * 
     * @param orderItem 저장할 주문 항목
     * @return 저장된 주문 항목 (ID가 할당된 상태)
     */
    OrderItem save(OrderItem orderItem);

    /**
     * 주문 항목 삭제
     * 
     * @param orderItem 삭제할 주문 항목
     */
    void delete(OrderItem orderItem);

    /**
     * ID로 주문 항목 삭제
     * 
     * @param id 삭제할 주문 항목 ID
     */
    void deleteById(Long id);

    /**
     * 주문 ID로 모든 항목 삭제
     * 
     * @param orderId 주문 ID
     */
    void deleteByOrderId(Long orderId);

    /**
     * 모든 주문 항목 조회 (관리자용)
     * 
     * @return 전체 주문 항목 목록
     */
    List<OrderItem> findAll();
}