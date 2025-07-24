package kr.hhplus.be.server.balance.repository;

import java.util.List;
import java.util.Optional;

import kr.hhplus.be.server.balance.domain.BalanceHistory;

/**
 * 잔액 이력 저장소 인터페이스
 */
public interface BalanceHistoryRepository {

    /**
     * 잔액 이력 저장
     * 
     * @param history 저장할 이력
     * @return 저장된 이력 (ID 할당됨)
     */
    BalanceHistory save(BalanceHistory history);

    /**
     * 사용자별 잔액 이력 조회 (최신순)
     * 
     * @param userId 사용자 ID
     * @return 잔액 변동 이력 목록
     */
    List<BalanceHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자별 최근 N개 이력 조회
     * 
     * @param userId 사용자 ID
     * @param limit  조회할 개수
     * @return 최근 이력 목록
     */
    List<BalanceHistory> findRecentHistoriesByUserId(Long userId, int limit);

    /**
     * 특정 거래 유형 이력 조회
     * 
     * @param userId          사용자 ID
     * @param transactionType 거래 유형
     * @return 해당 유형의 이력 목록
     */
    List<BalanceHistory> findByUserIdAndTransactionType(Long userId,
            BalanceHistory.TransactionType transactionType);

    /**
     * 거래 ID로 이력 조회 (중복 방지용)
     * 
     * @param transactionId 거래 ID
     * @return 해당 거래 이력
     */
    Optional<BalanceHistory> findByTransactionId(String transactionId);

    /**
     * ID로 이력 조회
     * 
     * @param id 이력 ID
     * @return 이력 정보
     */
    Optional<BalanceHistory> findById(Long id);

    /**
     * 모든 이력 조회 (관리자용)
     * 
     * @return 전체 이력 목록
     */
    List<BalanceHistory> findAll();
}