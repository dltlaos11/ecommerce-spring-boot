package kr.hhplus.be.server.balance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.balance.dto.BalanceHistoryResponse;
import kr.hhplus.be.server.balance.dto.BalanceResponse;
import kr.hhplus.be.server.balance.service.BalanceService;

/**
 * GetBalanceUseCase 단위 테스트
 * 
 * 테스트 전략:
 * - 조회 전용 UseCase의 단일 책임 확인
 * - readOnly 트랜잭션 적용 확인
 * - BalanceService에 올바른 위임 검증
 * 
 * 핵심 비즈니스 규칙:
 * - "사용자가 자신의 잔액을 조회한다"는 구체적인 요구사항 처리
 * - "사용자가 잔액 이력을 조회한다"는 구체적인 요구사항 처리
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetBalanceUseCase 단위 테스트")
class GetBalanceUseCaseTest {

    @Mock
    private BalanceService balanceService;

    @InjectMocks
    private GetBalanceUseCase getBalanceUseCase;

    @Test
    @DisplayName("잔액 조회 성공 - 사용자 ID로 잔액을 조회한다")
    void 잔액조회_성공() {
        // Given
        Long userId = 1L;
        BalanceResponse expectedResponse = new BalanceResponse(
                userId,
                new BigDecimal("50000"),
                LocalDateTime.now());

        when(balanceService.getUserBalance(userId)).thenReturn(expectedResponse);

        // When
        BalanceResponse response = getBalanceUseCase.execute(userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("50000"));

        verify(balanceService).getUserBalance(userId);
    }

    @Test
    @DisplayName("잔액 이력 조회 성공 - 사용자 ID와 limit으로 이력을 조회한다")
    void 잔액이력조회_성공() {
        // Given
        Long userId = 1L;
        int limit = 5;
        List<BalanceHistoryResponse> expectedHistories = List.of(
                new BalanceHistoryResponse(
                        "CHARGE",
                        new BigDecimal("30000"),
                        new BigDecimal("50000"),
                        LocalDateTime.now()),
                new BalanceHistoryResponse(
                        "PAYMENT",
                        new BigDecimal("10000"),
                        new BigDecimal("20000"),
                        LocalDateTime.now()));

        when(balanceService.getBalanceHistories(userId, limit)).thenReturn(expectedHistories);

        // When
        List<BalanceHistoryResponse> histories = getBalanceUseCase.executeHistoryQuery(userId, limit);

        // Then
        assertThat(histories).hasSize(2);
        assertThat(histories.get(0).transactionType()).isEqualTo("CHARGE");
        assertThat(histories.get(1).transactionType()).isEqualTo("PAYMENT");

        verify(balanceService).getBalanceHistories(userId, limit);
    }

    @Test
    @DisplayName("UseCase 단일 책임 검증 - 조회 관련 메서드만 존재한다")
    void UseCase_단일책임_검증() {
        // Given: UseCase 클래스 메서드 확인
        var methods = GetBalanceUseCase.class.getDeclaredMethods();
        var publicMethods = java.util.Arrays.stream(methods)
                .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
                .filter(method -> !method.getName().equals("equals"))
                .filter(method -> !method.getName().equals("hashCode"))
                .filter(method -> !method.getName().equals("toString"))
                .map(method -> method.getName())
                .sorted()
                .toList();

        // Then: execute와 executeHistoryQuery 메서드만 존재해야 함
        assertThat(publicMethods).containsExactly("execute", "executeHistoryQuery");
    }

    @Test
    @DisplayName("ReadOnly 트랜잭션 확인 - @Transactional(readOnly=true)가 적용되어 있다")
    void ReadOnly트랜잭션_확인() {
        // Given: UseCase 클래스
        Class<GetBalanceUseCase> clazz = GetBalanceUseCase.class;

        // Then: @Transactional(readOnly=true) 어노테이션이 적용되어 있어야 함
        var transactional = clazz.getAnnotation(
                org.springframework.transaction.annotation.Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
    }

    @Test
    @DisplayName("UseCase 어노테이션 확인 - @UseCase가 적용되어 있다")
    void UseCase_어노테이션_확인() {
        // Given: UseCase 클래스
        Class<GetBalanceUseCase> clazz = GetBalanceUseCase.class;

        // Then: @UseCase 어노테이션이 적용되어 있어야 함
        boolean hasUseCase = clazz.isAnnotationPresent(
                kr.hhplus.be.server.common.annotation.UseCase.class);

        assertThat(hasUseCase).isTrue();
    }

    @Test
    @DisplayName("의존성 주입 확인 - BalanceService만 의존한다")
    void 의존성주입_확인() {
        // Given: UseCase 필드 확인
        var fields = GetBalanceUseCase.class.getDeclaredFields();
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

    @Test
    @DisplayName("메서드 파라미터 검증 - execute와 executeHistoryQuery가 올바른 파라미터를 받는다")
    void 메서드파라미터_검증() throws NoSuchMethodException {
        // Given: 메서드 시그니처 확인
        var executeMethod = GetBalanceUseCase.class.getDeclaredMethod("execute", Long.class);
        var historyMethod = GetBalanceUseCase.class.getDeclaredMethod("executeHistoryQuery", Long.class, int.class);

        // Then: 메서드 시그니처가 올바른지 확인
        assertThat(executeMethod.getReturnType()).isEqualTo(BalanceResponse.class);
        assertThat(historyMethod.getReturnType()).isEqualTo(List.class);

        // 파라미터 타입 확인
        assertThat(executeMethod.getParameterTypes()).containsExactly(Long.class);
        assertThat(historyMethod.getParameterTypes()).containsExactly(Long.class, int.class);
    }
}