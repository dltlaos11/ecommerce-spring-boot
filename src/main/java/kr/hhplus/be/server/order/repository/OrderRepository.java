package kr.hhplus.be.server.order.repository;

import java.util.List;
import java.util.Optional;

import kr.hhplus.be.server.order.domain.Order;

/**
 * 주문 저장소 인터페이스 (Domain Layer)
 * DIP 적용 - Infrastructure가 이 인터페이스를 구현
 * 
 * 설계 원칙:
 * - DIP(의존성 역전 원칙) 적용
 * - 주문 조회 및 상태 관리 지원
 * - 테스트 시 Mock으로 쉽게 대체 가능
 * 
 * 책임:
 * - 주문 CRUD 기본 연산
 * - 사용자별 주문 목록 조회
 * - 주문 번호 기반 조회
 */
public interface OrderRepository {

    /**
     * ID로 주문 조회
     * 
     * @param id 주문 ID
     * @return 주문 정보 (Optional로 null 안전성 보장)
     */
    Optional<Order> findById(Long id);

    /**
     * 주문 번호로 주문 조회
     * 
     * @param orderNumber 주문 번호
     * @return 주문 정보
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * 사용자별 주문 목록 조회 (최신순)
     * 
     * @param userId 사용자 ID
     * @return 사용자의 주문 목록
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 모든 주문 목록 조회
     * 
     * @return 전체 주문 목록
     */
    List<Order> findAll();

    /**
     * 주문 저장 (생성 또는 수정)
     * 
     * @param order 저장할 주문
     * @return 저장된 주문 (ID가 할당된 상태)
     */
    Order save(Order order);

    /**
     * 주문 삭제
     * 
     * @param order 삭제할 주문
     */
    void delete(Order order);

    /**
     * ID로 주문 삭제
     * 
     * @param id 삭제할 주문 ID
     */
    void deleteById(Long id);

    /**
     * 특정 상태의 주문 목록 조회
     * 
     * @param status 주문 상태
     * @return 해당 상태의 주문 목록
     */
    List<Order> findByStatus(Order.OrderStatus status);

    /**
     * 사용자별 특정 상태 주문 조회
     * 
     * @param userId 사용자 ID
     * @param status 주문 상태
     * @return 해당 조건의 주문 목록
     */
    List<Order> findByUserIdAndStatus(Long userId, Order.OrderStatus status);
}
