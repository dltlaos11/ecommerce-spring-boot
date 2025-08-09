package kr.hhplus.be.server.balance.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.balance.dto.ChargeBalanceRequest;
import kr.hhplus.be.server.balance.repository.BalanceHistoryRepository;
import kr.hhplus.be.server.balance.repository.UserBalanceRepository;
import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.common.test.IntegrationTestBase;

/**
 * BalanceController 통합 테스트 - TestContainers 적용 버전
 */
@DisplayName("잔액 관리 통합 테스트")
@Transactional
class BalanceControllerIntegrationTest extends IntegrationTestBase {

        @Autowired
        private UserBalanceRepository userBalanceRepository;

        @Autowired
        private BalanceHistoryRepository balanceHistoryRepository;

        @BeforeEach
        void setUp() {
                verifyTestEnvironment();
        }

        @Test
        @DisplayName("사용자가 결제용 잔액을 미리 충전할 수 있다")
        void 사용자가_결제용_잔액을_미리_충전할_수_있다() {
                // Given
                Long userId = generateUniqueUserId();
                ChargeBalanceRequest request = new ChargeBalanceRequest(new BigDecimal("50000"));

                // When
                ResponseEntity<CommonResponse> response = restTemplate.postForEntity(
                                "/api/v1/users/{userId}/balance/charge",
                                request,
                                CommonResponse.class,
                                userId);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isTrue();

                // 잔액이 올바르게 충전되었는지 검증
                var savedBalance = userBalanceRepository.findByUserId(userId);
                assertThat(savedBalance).isPresent();
                assertThat(savedBalance.get().getBalance()).isEqualByComparingTo(new BigDecimal("50000"));

                // 충전 이력이 기록되었는지 검증
                var histories = balanceHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
                assertThat(histories).hasSize(1);
                assertThat(histories.get(0).getTransactionType()).isEqualTo(
                                kr.hhplus.be.server.balance.domain.BalanceHistory.TransactionType.CHARGE);
        }

        @Test
        @DisplayName("사용자가 현재 보유한 잔액을 조회할 수 있다")
        void 사용자가_현재_보유한_잔액을_조회할_수_있다() {
                // Given
                Long userId = generateUniqueUserId();
                ChargeBalanceRequest chargeRequest = new ChargeBalanceRequest(new BigDecimal("100000"));

                // 먼저 충전
                restTemplate.postForEntity(
                                "/api/v1/users/{userId}/balance/charge",
                                chargeRequest,
                                CommonResponse.class,
                                userId);

                // When
                ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                                "/api/v1/users/{userId}/balance",
                                CommonResponse.class,
                                userId);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isTrue();
                assertThat(response.getBody().getData()).isNotNull();
        }

        @Test
        @DisplayName("최소 충전 금액보다 적은 금액으로 충전을 시도하면 실패한다")
        void 최소_충전_금액보다_적은_금액으로_충전을_시도하면_실패한다() {
                // Given
                Long userId = generateUniqueUserId();
                ChargeBalanceRequest invalidRequest = new ChargeBalanceRequest(new BigDecimal("500")); // 최소 금액 미만

                // When
                ResponseEntity<CommonResponse> response = restTemplate.postForEntity(
                                "/api/v1/users/{userId}/balance/charge",
                                invalidRequest,
                                CommonResponse.class,
                                userId);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isFalse();

                // 잘못된 요청으로 인해 잔액이 생성되지 않았는지 확인
                var balance = userBalanceRepository.findByUserId(userId);
                assertThat(balance).isEmpty();
        }

        @Test
        @DisplayName("사용자가 과거 잔액 변동 이력을 조회할 수 있다")
        void 사용자가_과거_잔액_변동_이력을_조회할_수_있다() {
                // Given
                Long userId = generateUniqueUserId();

                // 첫 번째 충전
                restTemplate.postForEntity(
                                "/api/v1/users/{userId}/balance/charge",
                                new ChargeBalanceRequest(new BigDecimal("30000")),
                                CommonResponse.class,
                                userId);

                // 두 번째 충전
                restTemplate.postForEntity(
                                "/api/v1/users/{userId}/balance/charge",
                                new ChargeBalanceRequest(new BigDecimal("20000")),
                                CommonResponse.class,
                                userId);

                // When
                ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                                "/api/v1/users/{userId}/balance/history?limit=10",
                                CommonResponse.class,
                                userId);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isTrue();

                // 모든 충전 이력이 올바르게 기록되었는지 검증
                var histories = balanceHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
                assertThat(histories).hasSize(2);
        }
}