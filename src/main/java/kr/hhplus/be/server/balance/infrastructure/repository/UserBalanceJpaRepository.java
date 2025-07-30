package kr.hhplus.be.server.balance.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.balance.domain.UserBalance; //  통합된 Entity+Domain

/**
 * Entity-Domain 통합 버전 JPA Repository
 */
interface UserBalanceJpaRepository extends JpaRepository<UserBalance, Long> {

    /**
     * 사용자 ID로 잔액 조회
     */
    Optional<UserBalance> findByUserId(Long userId);

    /**
     * 낙관적 락으로 사용자 잔액 조회
     * 🔒 @Version과 함께 사용하여 동시성 제어
     */
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT ub FROM UserBalance ub WHERE ub.userId = :userId")
    Optional<UserBalance> findByUserIdWithOptimisticLock(@Param("userId") Long userId);

    /**
     * 비관적 락으로 사용자 잔액 조회 (필요시 사용)
     * 🔒 SELECT FOR UPDATE
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ub FROM UserBalance ub WHERE ub.userId = :userId")
    Optional<UserBalance> findByUserIdWithPessimisticLock(@Param("userId") Long userId);
}