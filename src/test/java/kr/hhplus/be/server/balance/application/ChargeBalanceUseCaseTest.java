package kr.hhplus.be.server.balance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.balance.dto.ChargeBalanceResponse;
import kr.hhplus.be.server.balance.exception.InvalidChargeAmountException;
import kr.hhplus.be.server.balance.service.BalanceService;

/**
 * ChargeBalanceUseCase 단위 테스트
 * 
 * 테스트 전략:
 * - UseCase는 단일 비즈니스 요구사항만 처리하는지 검증
 * - BalanceService에 올바른 파라미터로 위임하는지 검증
 * - 트랜잭션 어노테이션이 적용되어 있는지 확인 (컴파일 타임)
 * 
 * 핵심 비즈니스 규칙:
 * - "사용자가 잔액을 충전한다"는 하나의 구체적인 요구사항만 처리
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChargeBalanceUseCase 단위 테스트")
class ChargeBalanceUseCaseTest {

    @Mock
    private BalanceService balanceService;

    @InjectMocks
    private ChargeBalanceUseCase chargeBalanceUseCase;

    @Test
    @DisplayName("잔액 충전 성공 - 유효한 요청으로 충전할 때 BalanceService에 올바르게 위임한다")
    void 잔액충전_성공() {
        // Given
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("50000");

        ChargeBalanceResponse expectedResponse = new ChargeBalanceResponse(
                userId,
                new BigDecimal("20000"), // 이전 잔액
                amount,
                new BigDecimal("70000"), // 충전 후 잔액
                "TXN_TEST_123");

        when(balanceService.chargeBalance(userId, amount)).thenReturn(expectedResponse);

        // When
        ChargeBalanceResponse response = chargeBalanceUseCase.execute(userId, amount);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.chargedAmount()).isEqualByComparingTo(amount);
        assertThat(response.currentBalance()).isEqualByComparingTo(new BigDecimal("70000"));
        assertThat(response.transactionId()).isNotNull();

        // BalanceService에 정확한 파라미터로 위임했는지 검증
        verify(balanceService).chargeBalance(userId, amount);
    }

    @Test
    @DisplayName("잔액 충전 실패 - 잘못된 금액으로 충전할 때 예외를 전파한다")
    void 잔액충전_실패_잘못된금액() {
        // Given
        Long userId = 1L;
        BigDecimal invalidAmount = new BigDecimal("500"); // 최소 금액 미만

        when(balanceService.chargeBalance(userId, invalidAmount))
                .thenThrow(new InvalidChargeAmountException(
                        kr.hhplus.be.server.common.exception.ErrorCode.INVALID_CHARGE_AMOUNT));

        // When & Then
        assertThatThrownBy(() -> chargeBalanceUseCase.execute(userId, invalidAmount))
                .isInstanceOf(InvalidChargeAmountException.class);

        verify(balanceService).chargeBalance(userId, invalidAmount);
    }

    @Test
    @DisplayName("UseCase 단일 책임 검증 - execute 메서드만 존재한다")
    void UseCase_단일책임_검증() {
        // Given: UseCase 클래스 메서드 확인
        var methods = ChargeBalanceUseCase.class.getDeclaredMethods();
        var publicMethods = java.util.Arrays.stream(methods)
                .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
                .filter(method -> !method.getName().equals("equals"))
                .filter(method -> !method.getName().equals("hashCode"))
                .filter(method -> !method.getName().equals("toString"))
                .toList();

        // Then: execute 메서드만 존재해야 함 (단일 책임)
        assertThat(publicMethods).hasSize(1);
        assertThat(publicMethods.get(0).getName()).isEqualTo("execute");
    }

    @Test
    @DisplayName("트랜잭션 어노테이션 확인 - @Transactional이 적용되어 있다")
    void 트랜잭션_어노테이션_확인() {
        // Given: UseCase 클래스
        Class<ChargeBalanceUseCase> clazz = ChargeBalanceUseCase.class;

        // Then: @Transactional 어노테이션이 적용되어 있어야 함
        boolean hasTransactional = clazz.isAnnotationPresent(
                org.springframework.transaction.annotation.Transactional.class);

        assertThat(hasTransactional).isTrue();
    }

    @Test
    @DisplayName("UseCase 어노테이션 확인 - @UseCase가 적용되어 있다")
    void UseCase_어노테이션_확인() {
        // Given: UseCase 클래스
        Class<ChargeBalanceUseCase> clazz = ChargeBalanceUseCase.class;

        // Then: @UseCase 어노테이션이 적용되어 있어야 함
        boolean hasUseCase = clazz.isAnnotationPresent(
                kr.hhplus.be.server.common.annotation.UseCase.class);

        assertThat(hasUseCase).isTrue();
    }

    @Test
    @DisplayName("의존성 주입 확인 - BalanceService만 의존한다")
    void 의존성주입_확인() {
        // Given: UseCase 필드 확인
        var fields = ChargeBalanceUseCase.class.getDeclaredFields();
        var serviceFields = java.util.Arrays.stream(fields)
                .filter(field -> !field.getName().contains("mockito")) // Mockito 필드 제외
                .filter(field -> !field.getName().contains("Mock")) // Mock 필드 제외
                .filter(field -> !field.getName().equals("log")) // Lombok @Slf4j 필드 제외
                .filter(field -> !field.getType().equals(org.slf4j.Logger.class)) // Logger 타입 제외
                .toList();

        // Then: BalanceService 하나만 의존해야 함 (log 필드 제외)
        assertThat(serviceFields).hasSize(1);
        assertThat(serviceFields.get(0).getType()).isEqualTo(BalanceService.class);
        assertThat(serviceFields.get(0).getName()).isEqualTo("balanceService");
    }
}