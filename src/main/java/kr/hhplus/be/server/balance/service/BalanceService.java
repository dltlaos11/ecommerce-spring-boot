package kr.hhplus.be.server.balance.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.balance.domain.BalanceHistory;
import kr.hhplus.be.server.balance.domain.UserBalance;
import kr.hhplus.be.server.balance.dto.BalanceHistoryResponse;
import kr.hhplus.be.server.balance.dto.BalanceResponse;
import kr.hhplus.be.server.balance.dto.ChargeBalanceResponse;
import kr.hhplus.be.server.balance.repository.BalanceHistoryRepository;
import kr.hhplus.be.server.balance.repository.UserBalanceRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * 잔액 서비스
 * 
 * 설계 원칙:
 * - 단일 책임: 잔액 관련 비즈니스 로직만 처리
 * - 의존성 역전: Repository 인터페이스에만 의존
 * - 트랜잭션 관리로 데이터 일관성 보장
 * - STEP06에서 동시성 제어 및 재시도 로직 추가 예정
 * 
 * 책임:
 * - 잔액 충전/차감/조회
 * - 잔액 이력 관리
 * - DTO 변환
 */
@Slf4j
@Service
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션
public class BalanceService {

        private final UserBalanceRepository userBalanceRepository;
        private final BalanceHistoryRepository balanceHistoryRepository;

        /**
         * 생성자 주입 (스프링 권장 방식)
         */
        public BalanceService(UserBalanceRepository userBalanceRepository,
                        BalanceHistoryRepository balanceHistoryRepository) {
                this.userBalanceRepository = userBalanceRepository;
                this.balanceHistoryRepository = balanceHistoryRepository;
        }

        /**
         * 사용자 잔액 조회
         * 
         * @param userId 사용자 ID
         * @return 잔액 정보 DTO
         */
        public BalanceResponse getUserBalance(Long userId) {
                log.debug("💰 사용자 잔액 조회 요청: userId = {}", userId);

                UserBalance userBalance = userBalanceRepository.findByUserId(userId)
                                .orElseGet(() -> {
                                        log.info("🆕 새 사용자 잔액 생성: userId = {}", userId);
                                        return createNewUserBalance(userId);
                                });

                log.debug("✅ 사용자 잔액 조회 완료: userId = {}, balance = {}",
                                userId, userBalance.getBalance());

                return convertToBalanceResponse(userBalance);
        }

        /**
         * 잔액 충전
         * 
         * @param userId 사용자 ID
         * @param amount 충전할 금액
         * @return 충전 결과 DTO
         */
        @Transactional
        public ChargeBalanceResponse chargeBalance(Long userId, BigDecimal amount) {
                log.info("💳 잔액 충전 요청: userId = {}, amount = {}", userId, amount);

                // 1. 현재 잔액 조회
                UserBalance userBalance = userBalanceRepository.findByUserId(userId)
                                .orElseGet(() -> createNewUserBalance(userId));

                BigDecimal previousBalance = userBalance.getBalance();

                // 2. 도메인 객체의 비즈니스 로직 호출
                userBalance.charge(amount);

                // 3. 거래 ID 생성
                String transactionId = generateTransactionId("CHARGE");

                // 4. 잔액 저장
                UserBalance savedBalance = userBalanceRepository.save(userBalance);

                // 5. 이력 저장
                BalanceHistory history = BalanceHistory.createChargeHistory(
                                userId, amount, savedBalance.getBalance(), transactionId);
                balanceHistoryRepository.save(history);

                log.info("✅ 잔액 충전 완료: userId = {}, {} → {} (충전: {})",
                                userId, previousBalance, savedBalance.getBalance(), amount);

                return new ChargeBalanceResponse(
                                userId,
                                previousBalance,
                                amount,
                                savedBalance.getBalance(),
                                transactionId);
        }

        /**
         * 잔액 차감 (결제 시 호출)
         * 
         * @param userId  사용자 ID
         * @param amount  차감할 금액
         * @param orderId 주문 ID (이력 기록용)
         */
        @Transactional
        public void deductBalance(Long userId, BigDecimal amount, String orderId) {
                log.info("💸 잔액 차감 요청: userId = {}, amount = {}, orderId = {}",
                                userId, amount, orderId);

                // 1. 현재 잔액 조회
                UserBalance userBalance = userBalanceRepository.findByUserId(userId)
                                .orElseThrow(() -> {
                                        log.error("❌ 잔액 차감 실패 - 사용자 잔액 없음: userId = {}", userId);
                                        return new IllegalArgumentException("사용자 잔액을 찾을 수 없습니다.");
                                });

                BigDecimal previousBalance = userBalance.getBalance();

                // 2. 도메인 객체의 비즈니스 로직 호출
                userBalance.deduct(amount);

                // 3. 잔액 저장
                UserBalance savedBalance = userBalanceRepository.save(userBalance);

                // 4. 이력 저장
                BalanceHistory history = BalanceHistory.createPaymentHistory(
                                userId, amount, savedBalance.getBalance(), orderId);
                balanceHistoryRepository.save(history);

                log.info("✅ 잔액 차감 완료: userId = {}, {} → {} (차감: {})",
                                userId, previousBalance, savedBalance.getBalance(), amount);
        }

        /**
         * 잔액 환불 (결제 실패 시 호출)
         * 
         * @param userId  사용자 ID
         * @param amount  환불할 금액
         * @param orderId 주문 ID
         */
        @Transactional
        public void refundBalance(Long userId, BigDecimal amount, String orderId) {
                log.info("💰 잔액 환불 요청: userId = {}, amount = {}, orderId = {}",
                                userId, amount, orderId);

                // 1. 현재 잔액 조회
                UserBalance userBalance = userBalanceRepository.findByUserId(userId)
                                .orElseThrow(() -> {
                                        log.error("❌ 잔액 환불 실패 - 사용자 잔액 없음: userId = {}", userId);
                                        return new IllegalArgumentException("사용자 잔액을 찾을 수 없습니다.");
                                });

                BigDecimal previousBalance = userBalance.getBalance();

                // 2. 도메인 객체의 환불 로직 호출
                userBalance.refund(amount);

                // 3. 잔액 저장
                UserBalance savedBalance = userBalanceRepository.save(userBalance);

                // 4. 이력 저장
                BalanceHistory history = BalanceHistory.createRefundHistory(
                                userId, amount, savedBalance.getBalance(), orderId);
                balanceHistoryRepository.save(history);

                log.info("✅ 잔액 환불 완료: userId = {}, {} → {} (환불: {})",
                                userId, previousBalance, savedBalance.getBalance(), amount);
        }

        /**
         * 잔액 충분 여부 확인
         * 
         * @param userId 사용자 ID
         * @param amount 필요한 금액
         * @return 잔액 충분 여부
         */
        public boolean hasEnoughBalance(Long userId, BigDecimal amount) {
                log.debug("💳 잔액 충분 여부 확인: userId = {}, amount = {}", userId, amount);

                UserBalance userBalance = userBalanceRepository.findByUserId(userId)
                                .orElse(new UserBalance(userId)); // 잔액이 없으면 0으로 간주

                boolean hasEnough = userBalance.hasEnoughBalance(amount);

                log.debug("✅ 잔액 확인 결과: {} (현재 잔액: {})",
                                hasEnough ? "충분" : "부족", userBalance.getBalance());

                return hasEnough;
        }

        /**
         * 사용자 잔액 변동 이력 조회
         * 
         * @param userId 사용자 ID
         * @param limit  조회할 개수 (기본값: 10개)
         * @return 잔액 변동 이력 목록
         */
        public List<BalanceHistoryResponse> getBalanceHistories(Long userId, int limit) {
                log.debug("📋 잔액 이력 조회 요청: userId = {}, limit = {}", userId, limit);

                List<BalanceHistory> histories = balanceHistoryRepository
                                .findRecentHistoriesByUserId(userId, limit);

                log.debug("✅ 잔액 이력 조회 완료: userId = {}, {}개 이력", userId, histories.size());

                return histories.stream()
                                .map(this::convertToHistoryResponse)
                                .toList();
        }

        /**
         * 특정 거래 유형 이력 조회
         * 
         * @param userId          사용자 ID
         * @param transactionType 거래 유형
         * @return 해당 유형의 이력 목록
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
                                history.getTransactionType().getCode(),
                                history.getAmount(),
                                history.getBalanceAfter(),
                                history.getCreatedAt());
        }
}