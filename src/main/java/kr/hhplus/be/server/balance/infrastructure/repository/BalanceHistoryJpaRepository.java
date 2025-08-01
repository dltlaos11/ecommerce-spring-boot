package kr.hhplus.be.server.balance.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.hhplus.be.server.balance.domain.BalanceHistory; //  통합된 Entity+Domain

/**
 * Entity-Domain 통합 버전 JPA Repository
 */
public interface BalanceHistoryJpaRepository extends JpaRepository<BalanceHistory, Long> {

    /**
     * 사용자별 잔액 이력 조회 (최신순)
     */
    List<BalanceHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자별 최근 N개 이력 조회 (성능 최적화)
     */
    @Query("SELECT bh FROM BalanceHistory bh WHERE bh.userId = :userId ORDER BY bh.createdAt DESC")
    List<BalanceHistory> findRecentHistoriesByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 거래 유형 이력 조회
     */
    List<BalanceHistory> findByUserIdAndTransactionTypeOrderByCreatedAtDesc(
            Long userId, BalanceHistory.TransactionType transactionType);

    /**
     * 거래 ID로 이력 조회 (중복 방지용)
     */
    Optional<BalanceHistory> findByTransactionId(String transactionId);
}
