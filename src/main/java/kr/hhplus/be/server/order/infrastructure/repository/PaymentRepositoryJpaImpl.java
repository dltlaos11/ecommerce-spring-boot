package kr.hhplus.be.server.order.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.order.domain.Payment;
import kr.hhplus.be.server.order.repository.PaymentRepository;
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
public class PaymentRepositoryJpaImpl implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findByOrderId(Long orderId) {
        return jpaRepository.findByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Payment save(Payment payment) {
        log.debug("💾 결제 정보 저장: orderId = {}, amount = {}",
                payment.getOrderId(), payment.getAmount());

        // 변환 로직 없이 직접 저장
        return jpaRepository.save(payment);
    }

    @Override
    public void delete(Payment payment) {
        jpaRepository.delete(payment);
        log.debug("🗑️ 결제 정보 삭제: id = {}", payment.getId());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
        log.debug("🗑️ 결제 정보 삭제: id = {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findByStatus(Payment.PaymentStatus status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findByPaymentMethod(Payment.PaymentMethod paymentMethod) {
        return jpaRepository.findByPaymentMethod(paymentMethod);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findAll() {
        return jpaRepository.findAll();
    }
}