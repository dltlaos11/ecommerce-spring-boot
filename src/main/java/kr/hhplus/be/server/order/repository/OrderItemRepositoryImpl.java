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

import kr.hhplus.be.server.order.domain.OrderItem;

/**
 * 인메모리 주문 항목 저장소 구현체
 */
@Repository
public class OrderItemRepositoryImpl implements OrderItemRepository {

    private final Map<Long, OrderItem> orderItems = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<OrderItem> findById(Long id) {
        return Optional.ofNullable(orderItems.get(id));
    }

    @Override
    public List<OrderItem> findByOrderId(Long orderId) {
        return orderItems.values().stream()
                .filter(item -> item.getOrder().getId().equals(orderId))
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderItem> findByProductId(Long productId) {
        return orderItems.values().stream()
                .filter(item -> item.getProductId().equals(productId))
                .collect(Collectors.toList());
    }

    @Override
    public OrderItem save(OrderItem orderItem) {
        if (orderItem.getId() == null) {
            Long newId = idGenerator.getAndIncrement();
            orderItem.setId(newId);
        }

        orderItem.setCreatedAt(LocalDateTime.now());
        orderItems.put(orderItem.getId(), orderItem);

        return orderItem;
    }

    @Override
    public void delete(OrderItem orderItem) {
        if (orderItem.getId() != null) {
            orderItems.remove(orderItem.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        orderItems.remove(id);
    }

    @Override
    public void deleteByOrderId(Long orderId) {
        orderItems.values().removeIf(item -> item.getOrder() != null && item.getOrder().getId().equals(orderId));
    }

    @Override
    public List<OrderItem> findAll() {
        return new ArrayList<>(orderItems.values());
    }

    public void clear() {
        orderItems.clear();
        idGenerator.set(1);
    }

    public int count() {
        return orderItems.size();
    }
}