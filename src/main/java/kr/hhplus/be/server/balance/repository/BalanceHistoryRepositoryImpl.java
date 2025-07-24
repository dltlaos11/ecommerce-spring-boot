package kr.hhplus.be.server.balance.repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import kr.hhplus.be.server.balance.domain.BalanceHistory;

/**
 * 인메모리 잔액 이력 저장소 구현체
 */
@Repository
public class BalanceHistoryRepositoryImpl implements BalanceHistoryRepository {

    // 💾 인메모리 데이터 저장소
    private final Map<Long, BalanceHistory> histories = new ConcurrentHashMap<>();
    private final Map<String, BalanceHistory> historiesByTransactionId = new ConcurrentHashMap<>();

    // 🔢 ID 자동 생성기
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public BalanceHistory save(BalanceHistory history) {
        if (history.getId() == null) {
            // 새 이력: ID 자동 생성
            Long newId = idGenerator.getAndIncrement();
            history.setId(newId);
        }

        // 저장
        histories.put(history.getId(), history);

        // 거래 ID가 있는 경우 별도 맵에도 저장
        if (history.getTransactionId() != null) {
            historiesByTransactionId.put(history.getTransactionId(), history);
        }

        return history;
    }

    @Override
    public List<BalanceHistory> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return histories.values().stream()
                .filter(history -> history.getUserId().equals(userId))
                .sorted(Comparator.comparing(BalanceHistory::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<BalanceHistory> findRecentHistoriesByUserId(Long userId, int limit) {
        return findByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<BalanceHistory> findByUserIdAndTransactionType(Long userId,
            BalanceHistory.TransactionType transactionType) {
        return histories.values().stream()
                .filter(history -> history.getUserId().equals(userId) &&
                        history.getTransactionType() == transactionType)
                .sorted(Comparator.comparing(BalanceHistory::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BalanceHistory> findByTransactionId(String transactionId) {
        return Optional.ofNullable(historiesByTransactionId.get(transactionId));
    }

    @Override
    public Optional<BalanceHistory> findById(Long id) {
        return Optional.ofNullable(histories.get(id));
    }

    @Override
    public List<BalanceHistory> findAll() {
        return new ArrayList<>(histories.values());
    }

    /**
     * 테스트 및 개발용: 모든 데이터 초기화
     */
    public void clear() {
        histories.clear();
        historiesByTransactionId.clear();
        idGenerator.set(1);
        System.out.println("🗑️ 잔액 이력 데이터 모두 삭제됨");
    }

    /**
     * 현재 저장된 이력 수 반환 (디버깅용)
     */
    public int count() {
        return histories.size();
    }
}