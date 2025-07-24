package kr.hhplus.be.server.order.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import kr.hhplus.be.server.order.domain.Order;

/**
 * 인메모리 주문 저장소 구현체
 */
@Repository
public class OrderRepositoryImpl implements OrderRepository {

    // 💾 인메모리 데이터 저장소
    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final Map<String, Order> ordersByNumber = new ConcurrentHashMap<>();

    // 🔢 ID 자동 생성기
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return Optional.ofNullable(ordersByNumber.get(orderNumber));
    }

    @Override
    public List<Order> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return orders.values().stream()
                .filter(order -> order.getUserId().equals(userId))
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findAll() {
        return new ArrayList<>(orders.values());
    }

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            // 새 주문: ID 자동 생성
            Long newId = idGenerator.getAndIncrement();
            order.setId(newId);
        }

        // 저장 시간 갱신
        order.setUpdatedAt(LocalDateTime.now());

        // 저장 (ID와 주문번호 양쪽 맵에 저장)
        orders.put(order.getId(), order);
        ordersByNumber.put(order.getOrderNumber(), order);

        return order;
    }

    @Override
    public void delete(Order order) {
        if (order.getId() != null) {
            orders.remove(order.getId());
            ordersByNumber.remove(order.getOrderNumber());
        }
    }

    @Override
    public void deleteById(Long id) {
        Order order = orders.get(id);
        if (order != null) {
            orders.remove(id);
            ordersByNumber.remove(order.getOrderNumber());
        }
    }

    @Override
    public List<Order> findByStatus(Order.OrderStatus status) {
        return orders.values().stream()
                .filter(order -> order.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByUserIdAndStatus(Long userId, Order.OrderStatus status) {
        return orders.values().stream()
                .filter(order -> order.getUserId().equals(userId) && order.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * 테스트 및 개발용: 모든 데이터 초기화
     */
    public void clear() {
        orders.clear();
        ordersByNumber.clear();
        idGenerator.set(1);
        System.out.println("🗑️ 주문 데이터 모두 삭제됨");
    }

    /**
     * 현재 저장된 주문 수 반환 (디버깅용)
     */
    public int count() {
        return orders.size();
    }
}