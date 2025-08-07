package kr.hhplus.be.server.balance.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.balance.domain.BalanceHistory;
import kr.hhplus.be.server.balance.domain.UserBalance;
import kr.hhplus.be.server.balance.dto.BalanceHistoryResponse;
import kr.hhplus.be.server.balance.dto.BalanceResponse;
import kr.hhplus.be.server.balance.dto.ChargeBalanceResponse;
import kr.hhplus.be.server.balance.exception.BalanceConcurrencyException;
import kr.hhplus.be.server.balance.infrastructure.repository.UserBalanceJpaRepository;
import kr.hhplus.be.server.balance.repository.BalanceHistoryRepository;
import kr.hhplus.be.server.balance.repository.UserBalanceRepository;
import kr.hhplus.be.server.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

/**
 * 잔액 서비스 - 낙관적 락 구현
 * 
 * 동시성 제어 전략:
 * - 충전: 낙관적 락 + 재시도 (@Retryable)
 * - 차감: 비관적 락 (필요시 사용)
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class BalanceService {

        private final UserBalanceRepository userBalanceRepository;
        private final UserBalanceJpaRepository userBalanceJpaRepository; // 직접 접근용
        private final BalanceHistoryRepository balanceHistoryRepository;

        public BalanceService(UserBalanceRepository userBalanceRepository,
                        UserBalanceJpaRepository userBalanceJpaRepository,
                        BalanceHistoryRepository balanceHistoryRepository) {
                this.userBalanceRepository = userBalanceRepository;
                this.userBalanceJpaRepository = userBalanceJpaRepository;
                this.balanceHistoryRepository = balanceHistoryRepository;
        }

        /**
         * 사용자 잔액 조회
         */
        public BalanceResponse getUserBalance(Long userId) {
                UserBalance userBalance = userBalanceRepository.findByUserId(userId)
                                .orElseGet(() -> createNewUserBalance(userId));

                return convertToBalanceResponse(userBalance);
        }

        /**
         * 잔액 충전 - 낙관적 락 + 수동 재시도
         * 🔒 OptimisticLockingFailureException 발생 시 최대 3회 재시도
         */
        @Transactional
        public ChargeBalanceResponse chargeBalance(Long userId, BigDecimal amount) {
                log.info("🔒 낙관적 락 잔액 충전 시작: userId = {}, amount = {}", userId, amount);

                int maxAttempts = 3;
                int attempt = 0;

                while (attempt < maxAttempts) {
                        try {
                                attempt++;

                                // 낙관적 락으로 조회
                                UserBalance userBalance = userBalanceJpaRepository
                                                .findByUserIdWithOptimisticLock(userId)
                                                .orElseGet(() -> createNewUserBalance(userId));

                                BigDecimal previousBalance = userBalance.getBalance();

                                // 도메인 로직 실행
                                userBalance.charge(amount);

                                String transactionId = generateTransactionId("CHARGE");

                                // 저장 (버전 체크)
                                UserBalance savedBalance = userBalanceRepository.save(userBalance);

                                // 이력 저장
                                BalanceHistory history = BalanceHistory.createChargeHistory(
                                                userId, amount, savedBalance.getBalance(), transactionId);
                                balanceHistoryRepository.save(history);

                                log.info("✅ 낙관적 락 충전 성공 ({}회 시도): userId = {}, {} → {}",
                                                attempt, userId, previousBalance, savedBalance.getBalance());

                                return new ChargeBalanceResponse(
                                                userId, previousBalance, amount, savedBalance.getBalance(),
                                                transactionId);

                        } catch (OptimisticLockingFailureException e) {
                                log.warn("⚠️ 낙관적 락 충돌 발생 - 재시도 {}/{}: userId = {}", attempt, maxAttempts, userId);

                                if (attempt >= maxAttempts) {
                                        log.error("❌ 최대 재시도 횟수 초과: userId = {}", userId);
                                        throw new BalanceConcurrencyException(ErrorCode.BALANCE_CONCURRENCY_ERROR);
                                }

                                // 짧은 대기 후 재시도
                                try {
                                        Thread.sleep(50 * attempt); // 백오프 (50ms, 100ms, 150ms)
                                } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        throw new BalanceConcurrencyException(ErrorCode.BALANCE_CONCURRENCY_ERROR);
                                }
                        }
                }

                throw new BalanceConcurrencyException(ErrorCode.BALANCE_CONCURRENCY_ERROR);
        }

        /**
         * 잔액 차감 - 비관적 락 사용 (정확성 우선)
         */
        @Transactional
        public void deductBalance(Long userId, BigDecimal amount, String orderId) {
                log.info("🔒 비관적 락 잔액 차감 시작: userId = {}, amount = {}", userId, amount);

                // 비관적 락으로 조회
                UserBalance userBalance = userBalanceJpaRepository.findByUserIdWithPessimisticLock(userId)
                                .orElseThrow(() -> {
                                        log.error("잔액 차감 실패 - 사용자 잔액 없음: userId = {}", userId);
                                        return new IllegalArgumentException("사용자 잔액을 찾을 수 없습니다.");
                                });

                BigDecimal previousBalance = userBalance.getBalance();

                // 도메인 로직 실행
                userBalance.deduct(amount);

                // 저장
                UserBalance savedBalance = userBalanceRepository.save(userBalance);

                // 이력 저장
                BalanceHistory history = BalanceHistory.createPaymentHistory(
                                userId, amount, savedBalance.getBalance(), orderId);
                balanceHistoryRepository.save(history);

                log.info("✅ 비관적 락 차감 성공: userId = {}, {} → {}",
                                userId, previousBalance, savedBalance.getBalance());
        }

        /**
         * 잔액 환불 (결제 실패 시 호출)
         */
        @Transactional
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

        /**
         * 잔액 충분 여부 확인
         */
        public boolean hasEnoughBalance(Long userId, BigDecimal amount) {
                UserBalance userBalance = userBalanceRepository.findByUserId(userId)
                                .orElse(new UserBalance(userId));

                return userBalance.hasEnoughBalance(amount);
        }

        /**
         * 사용자 잔액 변동 이력 조회
         */
        public List<BalanceHistoryResponse> getBalanceHistories(Long userId, int limit) {
                List<BalanceHistory> histories = balanceHistoryRepository
                                .findRecentHistoriesByUserId(userId, limit);

                return histories.stream()
                                .map(this::convertToHistoryResponse)
                                .toList();
        }

        /**
         * 특정 거래 유형 이력 조회
         */
        public List<BalanceHistoryResponse> getBalanceHistoriesByType(Long userId,
                        BalanceHistory.TransactionType transactionType) {
                log.debug("📋 특정 유형 잔액 이력 조회: userId = {}, type = {}", userId, transactionType);

                List<BalanceHistory> histories = balanceHistoryRepository
                                .findByUserIdAndTransactionType(userId, transactionType);

                log.debug("✅ 특정 유형 이력 조회 완료: {}개", histories.size());

                return histories.stream()
                                .map(this::convertToHistoryResponse)
                                .toList();
        }

        /**
         * 동시성 안전한 잔액 충전 (낙관적 락 실패 시 명시적 예외)
         */
        @Transactional
        public ChargeBalanceResponse chargeBalanceWithConcurrencyControl(Long userId, BigDecimal amount) {
                log.info("🔒 동시성 제어 잔액 충전: userId = {}, amount = {}", userId, amount);

                int maxRetries = 3;
                int attempt = 0;

                while (attempt < maxRetries) {
                        try {
                                attempt++;

                                // 낙관적 락으로 조회
                                UserBalance userBalance = userBalanceJpaRepository
                                                .findByUserIdWithOptimisticLock(userId)
                                                .orElseGet(() -> createNewUserBalance(userId));

                                BigDecimal previousBalance = userBalance.getBalance();
                                userBalance.charge(amount);

                                String transactionId = generateTransactionId("CHARGE");
                                UserBalance savedBalance = userBalanceRepository.save(userBalance);

                                BalanceHistory history = BalanceHistory.createChargeHistory(
                                                userId, amount, savedBalance.getBalance(), transactionId);
                                balanceHistoryRepository.save(history);

                                log.info("✅ 동시성 제어 충전 성공 ({}회 시도): userId = {}", attempt, userId);

                                return new ChargeBalanceResponse(
                                                userId, previousBalance, amount, savedBalance.getBalance(),
                                                transactionId);

                        } catch (OptimisticLockingFailureException e) {
                                log.warn("⚠️ 낙관적 락 충돌 - 재시도 {}/{}: userId = {}", attempt, maxRetries, userId);

                                if (attempt >= maxRetries) {
                                        log.error("❌ 최대 재시도 횟수 초과: userId = {}", userId);
                                        throw new BalanceConcurrencyException(ErrorCode.BALANCE_CONCURRENCY_ERROR);
                                }

                                // 짧은 대기 후 재시도
                                try {
                                        Thread.sleep(50 * attempt); // 백오프
                                } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        throw new BalanceConcurrencyException(ErrorCode.BALANCE_CONCURRENCY_ERROR);
                                }
                        }
                }

                throw new BalanceConcurrencyException(ErrorCode.BALANCE_CONCURRENCY_ERROR);
        }

        /**
         * 새 사용자 잔액 생성 (내부용)
         */
        @Transactional
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