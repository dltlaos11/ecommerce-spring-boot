package kr.hhplus.be.server.balance.application;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.balance.dto.BalanceHistoryResponse;
import kr.hhplus.be.server.balance.dto.BalanceResponse;
import kr.hhplus.be.server.balance.dto.ChargeBalanceResponse;
import kr.hhplus.be.server.balance.service.BalanceService;
import kr.hhplus.be.server.common.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Application Layer - 잔액 비즈니스 Usecase
 * 
 * 책임:
 * - 잔액 관련 비즈니스 유스케이스 구현
 * - 트랜잭션 경계 관리
 * - 도메인 서비스 조합
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BalanceUseCase {

    private final BalanceService balanceService;

    /**
     * 잔액 조회 유스케이스
     */
    public BalanceResponse getUserBalance(Long userId) {
        log.debug("💰 잔액 조회 유스케이스: userId = {}", userId);
        return balanceService.getUserBalance(userId);
    }

    /**
     * 잔액 충전 유스케이스
     */
    @Transactional
    public ChargeBalanceResponse chargeBalance(Long userId, BigDecimal amount) {
        log.info("💳 잔액 충전 유스케이스: userId = {}, amount = {}", userId, amount);

        ChargeBalanceResponse response = balanceService.chargeBalance(userId, amount);

        log.info("✅ 잔액 충전 유스케이스 완료: userId = {}, 현재 잔액 = {}",
                userId, response.currentBalance());
        return response;
    }

    /**
     * 잔액 이력 조회 유스케이스
     */
    public List<BalanceHistoryResponse> getBalanceHistories(Long userId, int limit) {
        log.debug("📋 잔액 이력 조회 유스케이스: userId = {}, limit = {}", userId, limit);
        return balanceService.getBalanceHistories(userId, limit);
    }

    /**
     * 잔액 충분 여부 확인 유스케이스
     */
    public boolean hasEnoughBalance(Long userId, BigDecimal amount) {
        log.debug("💳 잔액 확인 유스케이스: userId = {}, amount = {}", userId, amount);
        return balanceService.hasEnoughBalance(userId, amount);
    }
}