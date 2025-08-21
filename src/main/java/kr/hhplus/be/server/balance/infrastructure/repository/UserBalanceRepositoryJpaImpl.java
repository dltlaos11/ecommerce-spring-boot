package kr.hhplus.be.server.balance.infrastructure.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.balance.domain.UserBalance; //  통합된 Entity+Domain
import kr.hhplus.be.server.balance.repository.BalanceHistoryRepository;
import kr.hhplus.be.server.balance.repository.UserBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Entity-Domain 통합 버전 Infrastructure 구현체
 * 
 * 핵심 변화:
 * - Entity ↔ Domain 변환 로직 제거
 * - JPA Repository에 직접 위임
 * - 코드 대폭 단순화
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional
public class UserBalanceRepositoryJpaImpl implements UserBalanceRepository {

    private final UserBalanceJpaRepository jpaRepository;
    private final BalanceHistoryRepository balanceHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserBalance> findByUserId(Long userId) {
        log.debug("💰 사용자 잔액 조회: userId = {}", userId);
        return jpaRepository.findByUserId(userId);
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRED)
    public UserBalance save(UserBalance userBalance) {
        log.debug("💾 사용자 잔액 저장: userId = {}, balance = {}",
                userBalance.getUserId(), userBalance.getBalance());

        // 변환 로직 없이 직접 저장
        return jpaRepository.save(userBalance);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserBalance> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserBalance> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    @Transactional
    public void delete(UserBalance userBalance) {
        jpaRepository.delete(userBalance);
        log.debug("🗑️ 사용자 잔액 삭제: id = {}", userBalance.getId());
    }

    @Override
    @Transactional
    public UserBalance saveWithHistory(UserBalance userBalance, kr.hhplus.be.server.balance.domain.BalanceHistory balanceHistory) {
        log.debug("💾 잔액 충전 트랜잭션 시작: userId = {}, balance = {}",
                userBalance.getUserId(), userBalance.getBalance());

        // 1. UserBalance 저장
        UserBalance savedBalance = jpaRepository.save(userBalance);
        log.debug("💾 사용자 잔액 저장 완료: userId = {}, balance = {}",
                savedBalance.getUserId(), savedBalance.getBalance());

        // 2. BalanceHistory 저장
        balanceHistoryRepository.save(balanceHistory);
        log.debug("📝 잔액 이력 저장 완료: userId = {}, type = {}, amount = {}",
                balanceHistory.getUserId(), balanceHistory.getTransactionType(), balanceHistory.getAmount());

        log.debug("✅ 잔액 충전 트랜잭션 완료: userId = {}", userBalance.getUserId());

        return savedBalance;
    }

    @Override
    @Transactional
    public void deductBalanceWithTransaction(Long userId, BigDecimal amount, String orderId) {
        log.debug("💳 잔액 차감 처리 시작: userId = {}, amount = {}, orderId = {}", userId, amount, orderId);
        
        // 1. 사용자 잔액 조회 (분산락 환경에서는 일반 조회)
        UserBalance userBalance = findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("잔액 차감 실패 - 사용자 잔액 없음: userId = {}", userId);
                    return new IllegalArgumentException("사용자 잔액을 찾을 수 없습니다.");
                });

        // 2. 잔액 차감
        userBalance.deduct(amount);
        UserBalance savedBalance = save(userBalance);

        // 3. 히스토리 저장
        kr.hhplus.be.server.balance.domain.BalanceHistory history = 
            kr.hhplus.be.server.balance.domain.BalanceHistory.createPaymentHistory(
                userId, amount, savedBalance.getBalance(), orderId);
        balanceHistoryRepository.save(history);
        
        log.debug("💳 잔액 차감 완료: userId = {}, 차감 후 잔액 = {}", userId, savedBalance.getBalance());
    }
}
