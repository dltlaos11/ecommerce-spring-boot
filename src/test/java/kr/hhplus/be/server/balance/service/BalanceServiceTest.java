package kr.hhplus.be.server.balance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.balance.domain.BalanceHistory;
import kr.hhplus.be.server.balance.domain.UserBalance;
import kr.hhplus.be.server.balance.dto.BalanceResponse;
import kr.hhplus.be.server.balance.dto.ChargeBalanceResponse;
import kr.hhplus.be.server.balance.exception.InsufficientBalanceException;
import kr.hhplus.be.server.balance.exception.InvalidChargeAmountException;
import kr.hhplus.be.server.balance.repository.BalanceHistoryRepository;
import kr.hhplus.be.server.balance.repository.UserBalanceRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("BalanceService 단위 테스트")
class BalanceServiceTest {

        @Mock
        private UserBalanceRepository userBalanceRepository;

        @Mock
        private BalanceHistoryRepository balanceHistoryRepository;

        @InjectMocks
        private BalanceService balanceService;

        @Test
        @DisplayName("잔액 조회 성공 - 기존 사용자의 잔액을 조회한다")
        void 잔액조회_성공_기존사용자() {
                // Given
                Long userId = 1L;
                UserBalance existingBalance = createTestUserBalance(userId, "50000.00");

                when(userBalanceRepository.findByUserId(userId))
                                .thenReturn(Optional.of(existingBalance));

                // When
                BalanceResponse response = balanceService.getUserBalance(userId);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.userId()).isEqualTo(userId);
                assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("50000.00"));

                verify(userBalanceRepository).findByUserId(userId);
                verify(userBalanceRepository, never()).save(any()); // 기존 사용자는 저장하지 않음
        }

        @Test
        @DisplayName("잔액 조회 성공 - 새 사용자의 경우 0원 잔액이 생성된다")
        void 잔액조회_성공_새사용자() {
                // Given
                Long userId = 999L;
                UserBalance newBalance = createTestUserBalance(userId, "0.00");

                when(userBalanceRepository.findByUserId(userId))
                                .thenReturn(Optional.empty());
                when(userBalanceRepository.save(any(UserBalance.class)))
                                .thenReturn(newBalance);

                // When
                BalanceResponse response = balanceService.getUserBalance(userId);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.userId()).isEqualTo(userId);
                assertThat(response.balance()).isEqualByComparingTo(BigDecimal.ZERO);

                verify(userBalanceRepository).findByUserId(userId);
                verify(userBalanceRepository).save(any(UserBalance.class));
        }

        @Test
        @DisplayName("잔액 충전 성공 - 유효한 금액으로 충전할 때 잔액이 증가한다")
        void 잔액충전_성공() {
                // Given
                Long userId = 1L;
                BigDecimal chargeAmount = new BigDecimal("30000.00");
                UserBalance userBalance = createTestUserBalance(userId, "20000.00");
                UserBalance savedBalance = createTestUserBalance(userId, "50000.00");

                when(userBalanceRepository.findByUserId(userId))
                                .thenReturn(Optional.of(userBalance));
                when(userBalanceRepository.save(any(UserBalance.class)))
                                .thenReturn(savedBalance);
                when(balanceHistoryRepository.save(any(BalanceHistory.class)))
                                .thenReturn(mock(BalanceHistory.class));

                // When
                ChargeBalanceResponse response = balanceService.chargeBalance(userId, chargeAmount);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.userId()).isEqualTo(userId);
                assertThat(response.previousBalance()).isEqualByComparingTo(new BigDecimal("20000.00"));
                assertThat(response.chargedAmount()).isEqualByComparingTo(chargeAmount);
                assertThat(response.currentBalance()).isEqualByComparingTo(new BigDecimal("50000.00"));
                assertThat(response.transactionId()).isNotNull();

                // Mock 호출 검증
                verify(userBalanceRepository).findByUserId(userId);
                verify(userBalanceRepository).save(any(UserBalance.class));
                verify(balanceHistoryRepository).save(any(BalanceHistory.class));
        }

        @Test
        @DisplayName("잔액 충전 실패 - 잘못된 금액(0원 이하)으로 충전 시 예외가 발생한다")
        void 잔액충전_실패_잘못된금액() {
                // Given
                Long userId = 1L;
                BigDecimal invalidAmount = BigDecimal.ZERO;
                UserBalance userBalance = createTestUserBalance(userId, "20000.00");

                when(userBalanceRepository.findByUserId(userId))
                                .thenReturn(Optional.of(userBalance));

                // When & Then
                assertThatThrownBy(() -> balanceService.chargeBalance(userId, invalidAmount))
                                .isInstanceOf(InvalidChargeAmountException.class);

                // save는 호출되지 않아야 함
                verify(userBalanceRepository, never()).save(any());
                verify(balanceHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("잔액 충전 실패 - 최소 충전 금액 미만으로 충전 시 예외가 발생한다")
        void 잔액충전_실패_최소금액미만() {
                // Given
                Long userId = 1L;
                BigDecimal tooSmallAmount = new BigDecimal("500.00"); // 최소 1000원
                UserBalance userBalance = createTestUserBalance(userId, "20000.00");

                when(userBalanceRepository.findByUserId(userId))
                                .thenReturn(Optional.of(userBalance));

                // When & Then
                assertThatThrownBy(() -> balanceService.chargeBalance(userId, tooSmallAmount))
                                .isInstanceOf(InvalidChargeAmountException.class);

                verify(userBalanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("잔액 충전 실패 - 최대 보유 한도 초과 시 예외가 발생한다")
        void 잔액충전_실패_최대한도초과() {
                // Given
                Long userId = 1L;
                BigDecimal chargeAmount = new BigDecimal("1000000.00");
                UserBalance userBalance = createTestUserBalance(userId, "9500000.00"); // 이미 많은 잔액 보유

                when(userBalanceRepository.findByUserId(userId))
                                .thenReturn(Optional.of(userBalance));

                // When & Then
                assertThatThrownBy(() -> balanceService.chargeBalance(userId, chargeAmount))
                                .isInstanceOf(InvalidChargeAmountException.class);

                verify(userBalanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("잔액 차감 성공 - 충분한 잔액이 있을 때 정상적으로 차감된다")
        void 잔액차감_성공() {
                // Given
                Long userId = 1L;
                BigDecimal deductAmount = new BigDecimal("20000.00");
                String orderId = "ORD-12345";
                UserBalance userBalance = createTestUserBalance(userId, "50000.00");
                UserBalance savedBalance = createTestUserBalance(userId, "30000.00");

                when(userBalanceRepository.findByUserId(userId))
                                .thenReturn(Optional.of(userBalance));
                when(userBalanceRepository.save(any(UserBalance.class)))
                                .thenReturn(savedBalance);
                when(balanceHistoryRepository.save(any(BalanceHistory.class)))
                                .thenReturn(mock(BalanceHistory.class));

                // When
                balanceService.deductBalance(userId, deductAmount, orderId);

                // Then: 도메인 객체의 상태 변경 확인
                assertThat(userBalance.getBalance()).isEqualByComparingTo(new BigDecimal("30000.00"));

                // Mock 호출 검증
                verify(userBalanceRepository).findByUserId(userId);
                verify(userBalanceRepository).save(userBalance);
                verify(balanceHistoryRepository).save(any(BalanceHistory.class));
        }

        @Test
        @DisplayName("잔액 차감 실패 - 잔액이 부족할 때 예외가 발생한다")
        void 잔액차감_실패_잔액부족() {
                // Given
                Long userId = 1L;
                BigDecimal deductAmount = new BigDecimal("60000.00"); // 잔액(50000)보다 큰 금액
                String orderId = "ORD-12345";
                UserBalance userBalance = createTestUserBalance(userId, "50000.00");

                when(userBalanceRepository.findByUserId(userId))
                                .thenReturn(Optional.of(userBalance));

                // When & Then
                assertThatThrownBy(() -> balanceService.deductBalance(userId, deductAmount, orderId))
                                .isInstanceOf(InsufficientBalanceException.class);

                // 잔액은 변하지 않아야 함
                assertThat(userBalance.getBalance()).isEqualByComparingTo(new BigDecimal("50000.00"));

                // save는 호출되지 않아야 함
                verify(userBalanceRepository, never()).save(any());
                verify(balanceHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("잔액 차감 실패 - 사용자 잔액이 존재하지 않을 때 예외가 발생한다")
        void 잔액차감_실패_사용자없음() {
                // Given
                Long userId = 999L;
                BigDecimal deductAmount = new BigDecimal("10000.00");
                String orderId = "ORD-12345";

                when(userBalanceRepository.findByUserId(userId))
                                .thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> balanceService.deductBalance(userId, deductAmount, orderId))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("사용자 잔액을 찾을 수 없습니다.");

                verify(userBalanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("잔액 환불 성공 - 결제 실패 시 잔액이 정상적으로 복구된다")
        void 잔액환불_성공() {
                // Given
                Long userId = 1L;
                BigDecimal refundAmount = new BigDecimal("15000.00");
                String orderId = "ORD-12345";
                UserBalance userBalance = createTestUserBalance(userId, "30000.00");
                UserBalance savedBalance = createTestUserBalance(userId, "45000.00");

                when(userBalanceRepository.findByUserId(userId))
                                .thenReturn(Optional.of(userBalance));
                when(userBalanceRepository.save(any(UserBalance.class)))
                                .thenReturn(savedBalance);
                when(balanceHistoryRepository.save(any(BalanceHistory.class)))
                                .thenReturn(mock(BalanceHistory.class));

                // When
                balanceService.refundBalance(userId, refundAmount, orderId);

                // Then: 도메인 객체의 상태 변경 확인
                assertThat(userBalance.getBalance()).isEqualByComparingTo(new BigDecimal("45000.00"));

                verify(userBalanceRepository).save(userBalance);
                verify(balanceHistoryRepository).save(any(BalanceHistory.class));
        }

        @Test
        @DisplayName("잔액 충분 여부 확인 성공 - 충분한 잔액이 있을 때 true를 반환한다")
        void 잔액충분여부확인_성공_충분한잔액() {
                // Given
                Long userId = 1L;
                BigDecimal requiredAmount = new BigDecimal("30000.00");
                UserBalance userBalance = createTestUserBalance(userId, "50000.00");

                when(userBalanceRepository.findByUserId(userId))
                                .thenReturn(Optional.of(userBalance));

                // When
                boolean hasEnough = balanceService.hasEnoughBalance(userId, requiredAmount);

                // Then
                assertThat(hasEnough).isTrue();
                verify(userBalanceRepository).findByUserId(userId);
        }

        @Test
        @DisplayName("잔액 충분 여부 확인 실패 - 잔액이 부족할 때 false를 반환한다")
        void 잔액충분여부확인_실패_잔액부족() {
                // Given
                Long userId = 1L;
                BigDecimal requiredAmount = new BigDecimal("60000.00");
                UserBalance userBalance = createTestUserBalance(userId, "50000.00");

                when(userBalanceRepository.findByUserId(userId))
                                .thenReturn(Optional.of(userBalance));

                // When
                boolean hasEnough = balanceService.hasEnoughBalance(userId, requiredAmount);

                // Then
                assertThat(hasEnough).isFalse();
        }

        @Test
        @DisplayName("잔액 충분 여부 확인 - 사용자 잔액이 없을 때 false를 반환한다")
        void 잔액충분여부확인_사용자없음() {
                // Given
                Long userId = 999L;
                BigDecimal requiredAmount = new BigDecimal("10000.00");

                when(userBalanceRepository.findByUserId(userId))
                                .thenReturn(Optional.empty());

                // When
                boolean hasEnough = balanceService.hasEnoughBalance(userId, requiredAmount);

                // Then
                assertThat(hasEnough).isFalse(); // 잔액이 없으면 0으로 간주하여 false
        }

        @Test
        @DisplayName("잔액 이력 조회 성공 - 사용자의 잔액 변동 이력을 조회한다")
        void 잔액이력조회_성공() {
                // Given
                Long userId = 1L;
                int limit = 5;
                List<BalanceHistory> mockHistories = List.of(
                                createTestBalanceHistory(userId, BalanceHistory.TransactionType.CHARGE, "30000.00"),
                                createTestBalanceHistory(userId, BalanceHistory.TransactionType.PAYMENT, "15000.00"));

                when(balanceHistoryRepository.findRecentHistoriesByUserId(userId, limit))
                                .thenReturn(mockHistories);

                // When
                var histories = balanceService.getBalanceHistories(userId, limit);

                // Then
                assertThat(histories).hasSize(2);

                // 수정된 부분: enum의 name() 메서드 결과와 비교
                assertThat(histories.get(0).transactionType()).isEqualTo("CHARGE");
                assertThat(histories.get(1).transactionType()).isEqualTo("PAYMENT");

                verify(balanceHistoryRepository).findRecentHistoriesByUserId(userId, limit);
        }

        @Test
        @DisplayName("특정 유형 잔액 이력 조회 성공 - 충전 이력만 조회한다")
        void 특정유형이력조회_성공() {
                // Given
                Long userId = 1L;
                BalanceHistory.TransactionType type = BalanceHistory.TransactionType.CHARGE;
                List<BalanceHistory> mockHistories = List.of(
                                createTestBalanceHistory(userId, type, "30000.00"),
                                createTestBalanceHistory(userId, type, "20000.00"));

                when(balanceHistoryRepository.findByUserIdAndTransactionType(userId, type))
                                .thenReturn(mockHistories);

                // When
                var histories = balanceService.getBalanceHistoriesByType(userId, type);

                // Then
                assertThat(histories).hasSize(2);
                assertThat(histories).allMatch(h -> h.transactionType().equals("CHARGE"));

                verify(balanceHistoryRepository).findByUserIdAndTransactionType(userId, type);
        }

        /**
         * 테스트용 UserBalance 객체 생성 헬퍼 메서드 - 수정된 버전
         */
        private UserBalance createTestUserBalance(Long userId, String balance) {
                UserBalance userBalance = new UserBalance(userId);

                // Reflection을 사용하여 private 필드에 접근 (테스트 전용)
                try {
                        java.lang.reflect.Field idField = UserBalance.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(userBalance, 1L);

                        java.lang.reflect.Field balanceField = UserBalance.class.getDeclaredField("balance");
                        balanceField.setAccessible(true);
                        balanceField.set(userBalance, new BigDecimal(balance));

                        java.lang.reflect.Field createdAtField = UserBalance.class.getDeclaredField("createdAt");
                        createdAtField.setAccessible(true);
                        createdAtField.set(userBalance, LocalDateTime.now());

                        java.lang.reflect.Field updatedAtField = UserBalance.class.getDeclaredField("updatedAt");
                        updatedAtField.setAccessible(true);
                        updatedAtField.set(userBalance, LocalDateTime.now());

                } catch (Exception e) {
                        throw new RuntimeException("테스트 데이터 생성 실패", e);
                }

                return userBalance;
        }

        /**
         * 테스트용 BalanceHistory 객체 생성 헬퍼 메서드 - 수정된 버전
         */
        private BalanceHistory createTestBalanceHistory(Long userId,
                        BalanceHistory.TransactionType type,
                        String amount) {
                // type에 따라 적절한 팩토리 메서드 사용
                BalanceHistory history;
                if (type == BalanceHistory.TransactionType.CHARGE) {
                        history = BalanceHistory.createChargeHistory(
                                        userId,
                                        new BigDecimal(amount),
                                        new BigDecimal("50000.00"),
                                        "TEST_TX_" + System.currentTimeMillis());
                } else {
                        history = BalanceHistory.createPaymentHistory(
                                        userId,
                                        new BigDecimal(amount),
                                        new BigDecimal("35000.00"),
                                        "TEST_ORDER_" + System.currentTimeMillis());
                }

                // Reflection으로 ID와 생성시간 설정
                try {
                        java.lang.reflect.Field idField = BalanceHistory.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(history, 1L);
                } catch (Exception e) {
                        throw new RuntimeException("테스트 데이터 생성 실패", e);
                }

                return history;
        }
}