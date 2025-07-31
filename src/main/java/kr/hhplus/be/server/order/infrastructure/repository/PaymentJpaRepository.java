package kr.hhplus.be.server.order.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.hhplus.be.server.order.domain.Payment;

/**
 * Payment JPA Repository (Infrastructure Layer)
 */
public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {

    /**
     * 주문별 결제 조회
     */
    Optional<Payment> findByOrderId(Long orderId);

    /**
     * 사용자별 결제 목록 조회 (최신순)
     */
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 상태의 결제 목록 조회
     */
    List<Payment> findByStatus(Payment.PaymentStatus status);

    /**
     * 결제 방법별 결제 목록 조회
     */
    List<Payment> findByPaymentMethod(Payment.PaymentMethod paymentMethod);
}