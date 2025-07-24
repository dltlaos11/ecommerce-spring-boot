package kr.hhplus.be.server.balance.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import kr.hhplus.be.server.balance.domain.UserBalance;

/**
 * 인메모리 사용자 잔액 저장소 구현체 (STEP05 기본 버전)
 * 
 * 기술적 특징:
 * - ConcurrentHashMap: 스레드 안전한 해시맵
 * - AtomicLong: 원자적 ID 생성
 * - 동시성 아직 미구현
 * 
 * STEP06에서 추가될 것:
 * - 낙관적 락 시뮬레이션 (version 충돌 감지)
 * - 재시도 메커니즘
 */
@Repository
public class UserBalanceRepositoryImpl implements UserBalanceRepository {

    // 인메모리 데이터 저장소
    private final Map<Long, UserBalance> balances = new ConcurrentHashMap<>();
    private final Map<Long, UserBalance> balancesByUserId = new ConcurrentHashMap<>();

    // ID 자동 생성기
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<UserBalance> findByUserId(Long userId) {
        return Optional.ofNullable(balancesByUserId.get(userId));
    }

    @Override
    public UserBalance save(UserBalance userBalance) {
        if (userBalance.getId() == null) {
            // 새 잔액: ID 자동 생성
            Long newId = idGenerator.getAndIncrement();
            userBalance.setId(newId);
        }

        // 저장 시간 갱신
        userBalance.setUpdatedAt(LocalDateTime.now());

        // 저장 (ID와 userId 양쪽 맵에 저장)
        balances.put(userBalance.getId(), userBalance);
        balancesByUserId.put(userBalance.getUserId(), userBalance);

        return userBalance;
    }

    @Override
    public Optional<UserBalance> findById(Long id) {
        return Optional.ofNullable(balances.get(id));
    }

    @Override
    public List<UserBalance> findAll() {
        return new ArrayList<>(balances.values());
    }

    @Override
    public void delete(UserBalance userBalance) {
        if (userBalance.getId() != null) {
            balances.remove(userBalance.getId());
            balancesByUserId.remove(userBalance.getUserId());
        }
    }

    /**
     * 애플리케이션 시작 시 테스트 데이터 자동 생성
     */
    @PostConstruct
    public void initData() {
        System.out.println("💰 사용자 잔액 테스트 데이터 초기화 시작...");

        // 테스트용 사용자 잔액 생성
        for (long userId = 1L; userId <= 5L; userId++) {
            UserBalance balance = new UserBalance(userId);
            save(balance);
        }

        System.out.println("✅ 사용자 잔액 테스트 데이터 초기화 완료! 총 " + balances.size() + "개 잔액");
    }

    /**
     * 테스트 및 개발용: 모든 데이터 초기화
     */
    public void clear() {
        balances.clear();
        balancesByUserId.clear();
        idGenerator.set(1);
        System.out.println("🗑️ 잔액 데이터 모두 삭제됨");
    }

    /**
     * 현재 저장된 잔액 수 반환 (디버깅용)
     */
    public int count() {
        return balances.size();
    }
}
