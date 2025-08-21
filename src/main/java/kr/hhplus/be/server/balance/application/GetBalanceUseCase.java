package kr.hhplus.be.server.balance.application;

import java.util.List;

import kr.hhplus.be.server.balance.dto.BalanceHistoryResponse;
import kr.hhplus.be.server.balance.dto.BalanceResponse;
import kr.hhplus.be.server.balance.service.BalanceService;
import kr.hhplus.be.server.common.annotation.UseCase;
import lombok.RequiredArgsConstructor;

/**
 * 잔액 조회 UseCase - 단일 비즈니스 요구사항만 처리
 * 
 * 구체적인 요구사항: "사용자가 자신의 잔액을 조회한다"
 */
@UseCase
@RequiredArgsConstructor
public class GetBalanceUseCase {

    private final BalanceService balanceService;

    /**
     * 잔액 조회 유스케이스 실행
     */
    public BalanceResponse execute(Long userId) {
        return balanceService.getUserBalance(userId);
    }

    /**
     * 잔액 이력 조회 유스케이스 실행
     */
    public List<BalanceHistoryResponse> executeHistoryQuery(Long userId, int limit) {
        return balanceService.getBalanceHistories(userId, limit);
    }
}