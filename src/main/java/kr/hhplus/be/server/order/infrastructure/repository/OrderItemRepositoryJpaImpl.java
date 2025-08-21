package kr.hhplus.be.server.order.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository 구현체 분리
 * Entity-Domain 통합으로 변환 로직 없음
 * N+1 문제 해결을 위한 배치 조회 메서드 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional
public class OrderItemRepositoryJpaImpl implements OrderItemRepository {

    private final OrderItemJpaRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderItem> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> findByOrderId(Long orderId) {
        return jpaRepository.findByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> findByProductId(Long productId) {
        return jpaRepository.findByProductId(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> findByOrderIdIn(List<Long> orderIds) {
        return jpaRepository.findByOrderIdIn(orderIds);
    }

    @Override
    @Transactional
    public OrderItem save(OrderItem orderItem) {
        log.debug("주문 항목 저장: orderId = {}, productId = {}, quantity = {}",
                orderItem.getOrderId(), orderItem.getProductId(), orderItem.getQuantity());

        return jpaRepository.save(orderItem);
    }

    @Override
    @Transactional
    public void delete(OrderItem orderItem) {
        jpaRepository.delete(orderItem);
        log.debug("주문 항목 삭제: id = {}", orderItem.getId());
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
        log.debug("주문 항목 삭제: id = {}", id);
    }

    @Override
    @Transactional
    public void deleteByOrderId(Long orderId) {
        jpaRepository.deleteByOrderId(orderId);
        log.debug("주문별 항목 삭제: orderId = {}", orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> findAll() {
        return jpaRepository.findAll();
    }
}