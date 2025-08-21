package kr.hhplus.be.server.balance.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import kr.hhplus.be.server.balance.domain.BalanceHistory;
import kr.hhplus.be.server.balance.domain.UserBalance;
import kr.hhplus.be.server.balance.dto.BalanceHistoryResponse;
import kr.hhplus.be.server.balance.dto.BalanceResponse;
import kr.hhplus.be.server.balance.dto.ChargeBalanceResponse;
import kr.hhplus.be.server.balance.repository.BalanceHistoryRepository;
import kr.hhplus.be.server.balance.repository.UserBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 분산락 기반 잔액 관리
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

        private final UserBalanceRepository userBalanceRepository;
        private final BalanceHistoryRepository balanceHistoryRepository;

        public BalanceResponse getUserBalance(Long userId) {
                UserBalance userBalance = userBalanceRepository.findByUserId(userId)
                                .orElseGet(() -> createNewUserBalance(userId));

                return convertToBalanceResponse(userBalance);
        }

        public ChargeBalanceResponse chargeBalance(Long userId, BigDecimal amount) {
                log.debug("💰 잔액 충전 시작: userId = {}, amount = {}", userId, amount);

                // 분산락 환경에서는 일반 조회 사용 (동시성은 분산락이 보장)
                UserBalance userBalance = userBalanceRepository.findByUserId(userId)
                                .orElseGet(() -> createNewUserBalance(userId));

                BigDecimal previousBalance = userBalance.getBalance();
                userBalance.charge(amount);

                String transactionId = generateTransactionId("CHARGE");

                // 인프라 레이어에서 트랜잭션 관리: UserBalance + BalanceHistory 동시 저장
                BalanceHistory history = BalanceHistory.createChargeHistory(
                                userId, amount, userBalance.getBalance(), transactionId);
                UserBalance savedBalance = userBalanceRepository.saveWithHistory(userBalance, history);

                log.debug("✅ 잔액 충전 완료: userId = {}, 이전잔액 = {}, 충전금액 = {}, 최종잔액 = {}",
                                userId, previousBalance, amount, savedBalance.getBalance());

                return new ChargeBalanceResponse(
                                userId, previousBalance, amount, savedBalance.getBalance(),
                                transactionId);
        }

        /**
         * 잔액 차감 - 분산락 사용 (비관적 락 대체)
         */
        public void deductBalance(Long userId, BigDecimal amount, String orderId) {
                // 인프라 레이어에서 트랜잭션과 함께 처리
                userBalanceRepository.deductBalanceWithTransaction(userId, amount, orderId);
        }

        public void refundBalance(Long userId, BigDecimal amount, String orderId) {
                UserBalance userBalance = userBalanceRepository.findByUserId(userId)
                                .orElseThrow(() -> {
                                        log.error("잔액 환불 실패 - 사용자 잔액 없음: userId = {}", userId);
                                        return new IllegalArgumentException("사용자 잔액을 찾을 수 없습니다.");
                                });

                userBalance.refund(amount);
                UserBalance savedBalance = userBalanceRepository.save(userBalance);

                BalanceHistory history = BalanceHistory.createRefundHistory(
                                userId, amount, savedBalance.getBalance(), orderId);
                balanceHistoryRepository.save(history);
        }

        public boolean hasEnoughBalance(Long userId, BigDecimal amount) {
                UserBalance userBalance = userBalanceRepository.findByUserId(userId)
                                .orElse(new UserBalance(userId));

                return userBalance.hasEnoughBalance(amount);
        }

        public List<BalanceHistoryResponse> getBalanceHistories(Long userId, int limit) {
                List<BalanceHistory> histories = balanceHistoryRepository
                                .findRecentHistoriesByUserId(userId, limit);

                return histories.stream()
                                .map(this::convertToHistoryResponse)
                                .toList();
        }

        public List<BalanceHistoryResponse> getBalanceHistoriesByType(Long userId,
                        BalanceHistory.TransactionType transactionType) {
                List<BalanceHistory> histories = balanceHistoryRepository
                                .findByUserIdAndTransactionType(userId, transactionType);

                return histories.stream()
                                .map(this::convertToHistoryResponse)
                                .toList();
        }

        /**
         * 동시성 안전한 잔액 충전 (분산락 기반)
         */
        public ChargeBalanceResponse chargeBalanceWithConcurrencyControl(Long userId, BigDecimal amount) {
                log.info("🔒 동시성 제어 잔액 충전: userId = {}, amount = {}", userId, amount);

                // 분산락 환경에서는 일반 조회 사용 (동시성은 분산락이 보장)
                UserBalance userBalance = userBalanceRepository.findByUserId(userId)
                                .orElseGet(() -> createNewUserBalance(userId));

                BigDecimal previousBalance = userBalance.getBalance();
                userBalance.charge(amount);

                String transactionId = generateTransactionId("CHARGE");
                UserBalance savedBalance = userBalanceRepository.save(userBalance);

                BalanceHistory history = BalanceHistory.createChargeHistory(
                                userId, amount, savedBalance.getBalance(), transactionId);
                balanceHistoryRepository.save(history);

                log.info("✅ 동시성 제어 잔액 충전 완료: userId = {}, 최종잔액 = {}",
                                userId, savedBalance.getBalance());

                return new ChargeBalanceResponse(
                                userId, previousBalance, amount, savedBalance.getBalance(),
                                transactionId);
        }

        /**
         * 새 사용자 잔액 생성 (내부용)
         */
        private UserBalance createNewUserBalance(Long userId) {
                UserBalance newBalance = new UserBalance(userId);
                return userBalanceRepository.save(newBalance);
        }

        /**
         * 거래 ID 생성
         */
        private String generateTransactionId(String prefix) {
                return String.format("%s_%d_%s",
                                prefix,
                                System.currentTimeMillis(),
                                UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        /**
         * UserBalance를 BalanceResponse DTO로 변환
         */
        private BalanceResponse convertToBalanceResponse(UserBalance userBalance) {
                return new BalanceResponse(
                                userBalance.getUserId(),
                                userBalance.getBalance(),
                                userBalance.getUpdatedAt());
        }

        /**
         * BalanceHistory를 BalanceHistoryResponse DTO로 변환
         */
        private BalanceHistoryResponse convertToHistoryResponse(BalanceHistory history) {
                return new BalanceHistoryResponse(
                                history.getTransactionType().name(),
                                history.getAmount(),
                                history.getBalanceAfter(),
                                history.getCreatedAt());
        }
}