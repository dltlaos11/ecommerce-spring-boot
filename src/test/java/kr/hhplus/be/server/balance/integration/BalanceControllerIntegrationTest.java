package kr.hhplus.be.server.balance.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

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
 * BalanceController 통합 테스트 - 수정된 버전
 * 
 * 수정사항:
 * - 고유한 사용자 ID 사용으로 테스트 간 간섭 방지
 * - @Sql 어노테이션으로 데이터 정리 자동화
 * - 더 안전한 테스트 환경 구성
 */
@DisplayName("잔액 관리 통합 테스트")
@Transactional
class BalanceControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private BalanceHistoryRepository balanceHistoryRepository;

    @Test
    @DisplayName("잔액 충전 통합 테스트 - 전체 플로우가 정상 동작한다")
    void 잔액충전_통합테스트() {
        // Given: 고유한 사용자 ID 사용
        Long userId = generateUniqueUserId();
        ChargeBalanceRequest request = new ChargeBalanceRequest(new BigDecimal("50000"));

        // When: API 호출
        ResponseEntity<CommonResponse> response = restTemplate.postForEntity(
                "/api/v1/users/{userId}/balance/charge",
                request,
                CommonResponse.class,
                userId);

        // Then: HTTP 응답 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().isSuccess()).isTrue();

        // DB 상태 검증
        var savedBalance = userBalanceRepository.findByUserId(userId);
        assertThat(savedBalance).isPresent();
        assertThat(savedBalance.get().getBalance()).isEqualByComparingTo(new BigDecimal("50000"));

        // 이력 저장 검증
        var histories = balanceHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getTransactionType()).isEqualTo(
                kr.hhplus.be.server.balance.domain.BalanceHistory.TransactionType.CHARGE);
    }

    @Test
    @DisplayName("잔액 조회 통합 테스트 - 기존 잔액이 정확히 조회된다")
    void 잔액조회_통합테스트() {
        // Given: 미리 잔액 충전
        Long userId = generateUniqueUserId();
        ChargeBalanceRequest chargeRequest = new ChargeBalanceRequest(new BigDecimal("100000"));
        restTemplate.postForEntity(
                "/api/v1/users/{userId}/balance/charge",
                chargeRequest,
                CommonResponse.class,
                userId);

        // When: 잔액 조회 API 호출
        ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                "/api/v1/users/{userId}/balance",
                CommonResponse.class,
                userId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        // 응답 데이터 검증은 실제 응답 구조에 맞게 조정 필요
        assertThat(response.getBody().getData()).isNotNull();
    }

    @Test
    @DisplayName("잘못된 충전 금액으로 요청 시 400 에러가 발생한다")
    void 잘못된충전금액_400에러() {
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
        assertThat(response.getBody().isSuccess()).isFalse();

        // DB에 잔액이 생성되지 않았는지 확인
        var balance = userBalanceRepository.findByUserId(userId);
        assertThat(balance).isEmpty();
    }

    @Test
    @DisplayName("잔액 이력 조회 통합 테스트")
    void 잔액이력조회_통합테스트() {
        // Given: 여러 번 충전
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

        // When: 이력 조회
        ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                "/api/v1/users/{userId}/balance/history?limit=10",
                CommonResponse.class,
                userId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        // DB 검증 - 2개의 이력이 있어야 함
        var histories = balanceHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(histories).hasSize(2);
    }
}