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
                try {
                        verifyTestEnvironment();
                        System.out.println("🧪 Balance test setup completed");
                } catch (Exception e) {
                        debugTestFailure("setUp", e);
                        throw e;
                }
        }

        @Test
        @DisplayName("잔액 충전 통합 테스트 - 전체 플로우가 정상 동작한다")
        void 잔액충전_통합테스트() {
                try {
                        // Given
                        Long userId = generateUniqueUserId();
                        ChargeBalanceRequest request = new ChargeBalanceRequest(new BigDecimal("50000"));

                        System.out.println("💳 테스트 시작 - 사용자 ID: " + userId);

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

                        System.out.println("✅ API 응답 성공");

                        // DB 상태 검증
                        var savedBalance = userBalanceRepository.findByUserId(userId);
                        assertThat(savedBalance).isPresent();
                        assertThat(savedBalance.get().getBalance()).isEqualByComparingTo(new BigDecimal("50000"));

                        // 이력 저장 검증
                        var histories = balanceHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
                        assertThat(histories).hasSize(1);
                        assertThat(histories.get(0).getTransactionType()).isEqualTo(
                                        kr.hhplus.be.server.balance.domain.BalanceHistory.TransactionType.CHARGE);

                        System.out.println("✅ DB 검증 완료");

                } catch (Exception e) {
                        debugTestFailure("잔액충전_통합테스트", e);
                        throw e;
                }
        }

        @Test
        @DisplayName("잔액 조회 통합 테스트 - 기존 잔액이 정확히 조회된다")
        void 잔액조회_통합테스트() {
                try {
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

                } catch (Exception e) {
                        debugTestFailure("잔액조회_통합테스트", e);
                        throw e;
                }
        }

        @Test
        @DisplayName("잘못된 충전 금액으로 요청 시 400 에러가 발생한다")
        void 잘못된충전금액_400에러() {
                try {
                        // Given
                        Long userId = generateUniqueUserId();
                        ChargeBalanceRequest invalidRequest = new ChargeBalanceRequest(new BigDecimal("500")); // 최소 금액
                                                                                                               // 미만

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

                        // DB에 잔액이 생성되지 않았는지 확인
                        var balance = userBalanceRepository.findByUserId(userId);
                        assertThat(balance).isEmpty();

                } catch (Exception e) {
                        debugTestFailure("잘못된충전금액_400에러", e);
                        throw e;
                }
        }

        @Test
        @DisplayName("잔액 이력 조회 통합 테스트")
        void 잔액이력조회_통합테스트() {
                try {
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

                        // DB 검증
                        var histories = balanceHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
                        assertThat(histories).hasSize(2);

                } catch (Exception e) {
                        debugTestFailure("잔액이력조회_통합테스트", e);
                        throw e;
                }
        }
}