package kr.hhplus.be.server.order.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository 구현체 분리
 * Entity-Domain 통합으로 변환 로직 없음
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional
public class OrderRepositoryJpaImpl implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return jpaRepository.findByOrderNumber(orderNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Order save(Order order) {
        log.debug("💾 주문 저장: orderNumber = {}, userId = {}",
                order.getOrderNumber(), order.getUserId());

        // 변환 로직 없이 직접 저장
        return jpaRepository.save(order);
    }

    @Override
    public void delete(Order order) {
        jpaRepository.delete(order);
        log.debug("🗑️ 주문 삭제: id = {}", order.getId());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
        log.debug("🗑️ 주문 삭제: id = {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByStatus(Order.OrderStatus status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByUserIdAndStatus(Long userId, Order.OrderStatus status) {
        return jpaRepository.findByUserIdAndStatus(userId, status);
    }
}