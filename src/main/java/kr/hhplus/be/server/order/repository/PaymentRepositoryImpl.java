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

import kr.hhplus.be.server.order.domain.Payment;

/**
 * 인메모리 결제 저장소 구현체
 */
@Repository
public class PaymentRepositoryImpl implements PaymentRepository {

    private final Map<Long, Payment> payments = new ConcurrentHashMap<>();
    private final Map<Long, Payment> paymentsByOrderId = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<Payment> findById(Long id) {
        return Optional.ofNullable(payments.get(id));
    }

    @Override
    public Optional<Payment> findByOrderId(Long orderId) {
        return Optional.ofNullable(paymentsByOrderId.get(orderId));
    }

    @Override
    public List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return payments.values().stream()
                .filter(payment -> payment.getUserId().equals(userId))
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public Payment save(Payment payment) {
        if (payment.getId() == null) {
            Long newId = idGenerator.getAndIncrement();
            payment.setId(newId);
        }

        payment.setUpdatedAt(LocalDateTime.now());

        // 저장 (ID와 주문ID 양쪽 맵에 저장)
        payments.put(payment.getId(), payment);
        paymentsByOrderId.put(payment.getOrderId(), payment);

        return payment;
    }

    @Override
    public void delete(Payment payment) {
        if (payment.getId() != null) {
            payments.remove(payment.getId());
            paymentsByOrderId.remove(payment.getOrderId());
        }
    }

    @Override
    public void deleteById(Long id) {
        Payment payment = payments.get(id);
        if (payment != null) {
            payments.remove(id);
            paymentsByOrderId.remove(payment.getOrderId());
        }
    }

    @Override
    public List<Payment> findByStatus(Payment.PaymentStatus status) {
        return payments.values().stream()
                .filter(payment -> payment.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<Payment> findByPaymentMethod(Payment.PaymentMethod paymentMethod) {
        return payments.values().stream()
                .filter(payment -> payment.getPaymentMethod() == paymentMethod)
                .collect(Collectors.toList());
    }

    @Override
    public List<Payment> findAll() {
        return new ArrayList<>(payments.values());
    }

    public void clear() {
        payments.clear();
        paymentsByOrderId.clear();
        idGenerator.set(1);
    }

    public int count() {
        return payments.size();
    }
}