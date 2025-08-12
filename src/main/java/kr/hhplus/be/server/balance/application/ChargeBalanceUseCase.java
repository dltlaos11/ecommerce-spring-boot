package kr.hhplus.be.server.balance.application;

import java.math.BigDecimal;

import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.balance.dto.ChargeBalanceResponse;
import kr.hhplus.be.server.balance.service.BalanceService;
import kr.hhplus.be.server.common.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 잔액 충전 UseCase - 단일 비즈니스 요구사항만 처리
 * 
 * 하나의 구체적인 요구사항: "사용자가 잔액을 충전한다"
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class ChargeBalanceUseCase {

    private final BalanceService balanceService;

    public ChargeBalanceResponse execute(Long userId, BigDecimal amount) {
        return balanceService.chargeBalance(userId, amount);
    }
}