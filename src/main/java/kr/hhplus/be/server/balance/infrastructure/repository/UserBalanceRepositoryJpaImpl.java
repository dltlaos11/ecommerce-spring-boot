package kr.hhplus.be.server.balance.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.balance.domain.UserBalance; //  통합된 Entity+Domain
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

    @Override
    @Transactional(readOnly = true)
    public Optional<UserBalance> findByUserId(Long userId) {
        log.debug("💰 사용자 잔액 조회: userId = {}", userId);
        return jpaRepository.findByUserId(userId);
    }

    @Override
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
    public void delete(UserBalance userBalance) {
        jpaRepository.delete(userBalance);
        log.debug("🗑️ 사용자 잔액 삭제: id = {}", userBalance.getId());
    }
}
