package kr.hhplus.be.server.order.repository;

import java.util.List;
import java.util.Optional;

import kr.hhplus.be.server.order.domain.Payment;

/**
 * 결제 저장소 인터페이스
 * 
 * 설계 원칙:
 * - DIP(의존성 역전 원칙) 적용
 * - 결제 이력 관리 지원
 * - 주문별 결제 정보 추적
 * 
 * 책임:
 * - 결제 CRUD 기본 연산
 * - 주문별 결제 정보 조회
 * - 사용자별 결제 이력 조회
 */
public interface PaymentRepository {

    /**
     * ID로 결제 조회
     * 
     * @param id 결제 ID
     * @return 결제 정보 (Optional로 null 안전성 보장)
     */
    Optional<Payment> findById(Long id);

    /**
     * 주문별 결제 조회
     * 
     * @param orderId 주문 ID
     * @return 해당 주문의 결제 정보
     */
    Optional<Payment> findByOrderId(Long orderId);

    /**
     * 사용자별 결제 목록 조회 (최신순)
     * 
     * @param userId 사용자 ID
     * @return 사용자의 결제 목록
     */
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 결제 저장 (생성 또는 수정)
     * 
     * @param payment 저장할 결제
     * @return 저장된 결제 (ID가 할당된 상태)
     */
    Payment save(Payment payment);

    /**
     * 결제 삭제
     * 
     * @param payment 삭제할 결제
     */
    void delete(Payment payment);

    /**
     * ID로 결제 삭제
     * 
     * @param id 삭제할 결제 ID
     */
    void deleteById(Long id);

    /**
     * 특정 상태의 결제 목록 조회
     * 
     * @param status 결제 상태
     * @return 해당 상태의 결제 목록
     */
    List<Payment> findByStatus(Payment.PaymentStatus status);

    /**
     * 결제 방법별 결제 목록 조회
     * 
     * @param paymentMethod 결제 방법
     * @return 해당 결제 방법의 결제 목록
     */
    List<Payment> findByPaymentMethod(Payment.PaymentMethod paymentMethod);

    /**
     * 모든 결제 목록 조회 (관리자용)
     * 
     * @return 전체 결제 목록
     */
    List<Payment> findAll();
}