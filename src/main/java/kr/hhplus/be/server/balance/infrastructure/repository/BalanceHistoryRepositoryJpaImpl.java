package kr.hhplus.be.server.balance.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.balance.domain.BalanceHistory; //  통합된 Entity+Domain
import kr.hhplus.be.server.balance.repository.BalanceHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Entity-Domain 통합 버전 Infrastructure 구현체
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional
public class BalanceHistoryRepositoryJpaImpl implements BalanceHistoryRepository {

    private final BalanceHistoryJpaRepository jpaRepository;

    @Override
    public BalanceHistory save(BalanceHistory history) {
        log.debug("📝 잔액 이력 저장: userId = {}, type = {}, amount = {}",
                history.getUserId(), history.getTransactionType(), history.getAmount());

        // 변환 로직 없이 직접 저장
        return jpaRepository.save(history);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BalanceHistory> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BalanceHistory> findRecentHistoriesByUserId(Long userId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        return jpaRepository.findRecentHistoriesByUserId(userId, pageRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BalanceHistory> findByUserIdAndTransactionType(Long userId,
            BalanceHistory.TransactionType transactionType) {
        return jpaRepository.findByUserIdAndTransactionTypeOrderByCreatedAtDesc(userId, transactionType);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BalanceHistory> findByTransactionId(String transactionId) {
        return jpaRepository.findByTransactionId(transactionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BalanceHistory> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BalanceHistory> findAll() {
        return jpaRepository.findAll();
    }
}