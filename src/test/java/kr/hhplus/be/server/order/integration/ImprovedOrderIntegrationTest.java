package kr.hhplus.be.server.order.integration;

import kr.hhplus.be.server.balance.dto.BalanceResponse;
import kr.hhplus.be.server.balance.dto.ChargeBalanceResponse;
import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.common.test.IntegrationTestBase;
import kr.hhplus.be.server.order.dto.OrderResponse;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.support.BusinessTestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * 비즈니스 중심 주문 통합 테스트
 * 
 * 개선사항:
 * - Repository 직접 의존성 제거
 * - API 기반 비즈니스 행동 검증
 * - 구현 세부사항에서 분리
 * - "What" 중심 테스트 (How 숨김)
 */
@DisplayName("주문 비즈니스 통합 테스트")
class ImprovedOrderIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BusinessTestHelper businessHelper;

    @Test
    @DisplayName("사용자가 충분한 잔액으로 상품을 주문할 수 있다")
    void 충분한_잔액으로_상품_주문_성공() {
        // Given: 사용자가 충분한 잔액을 보유하고, 재고가 충분한 상품이 존재
        Long userId = businessHelper.테스트_사용자_생성();
        BigDecimal 충전금액 = new BigDecimal("1000000");
        ProductResponse 상품 = businessHelper.재고_충분한_상품_선택(1);
        
        // 잔액 충전 (여러 번 충전하여 충분한 금액 확보)
        ChargeBalanceResponse 충전결과1 = businessHelper.잔액_충전(userId, 충전금액);
        ChargeBalanceResponse 충전결과2 = businessHelper.잔액_충전(userId, 충전금액);
        
        // When: 사용자가 상품을 주문
        OrderResponse 주문결과 = businessHelper.주문_생성(userId, 상품.id(), 1);
        
        // Then: 주문이 성공하고, 잔액이 정확히 차감됨
        assertAll(
            () -> assertThat(주문결과).isNotNull(),
            () -> assertThat(주문결과.userId()).isEqualTo(userId),
            () -> assertThat(주문결과.status()).isEqualTo("COMPLETED"),
            () -> {
                BalanceResponse 차감후잔액 = businessHelper.잔액_조회(userId);
                BigDecimal 총충전금액 = 충전금액.multiply(new BigDecimal("2"));
                BigDecimal 예상잔액 = 총충전금액.subtract(상품.price());
                assertThat(차감후잔액.balance()).isEqualByComparingTo(예상잔액);
            }
        );
    }

    @Test
    @DisplayName("잔액이 부족하면 주문을 완료할 수 없다")
    void 잔액_부족시_주문_실패() {
        // Given: 사용자가 부족한 잔액을 보유
        Long userId = businessHelper.테스트_사용자_생성();
        BigDecimal 부족한금액 = new BigDecimal("1000");
        ProductResponse 상품 = businessHelper.재고_충분한_상품_선택(1);
        
        // 부족한 금액만 충전
        businessHelper.잔액_충전(userId, 부족한금액);
        
        // When: 비싼 상품을 주문 시도
        ResponseEntity<CommonResponse<OrderResponse>> 주문응답 = 
            businessHelper.주문_생성_시도(userId, 상품.id(), 1);
        
        // Then: 주문이 실패하고, 잔액은 변경되지 않음
        assertAll(
            () -> assertThat(주문응답.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
            () -> {
                BalanceResponse 주문후잔액 = businessHelper.잔액_조회(userId);
                assertThat(주문후잔액.balance()).isEqualByComparingTo(부족한금액);
            }
        );
    }

    @Test
    @DisplayName("재고가 부족한 상품으로 주문하려고 하면 실패한다")
    void 재고_부족시_주문_실패() {
        // Given: 사용자가 충분한 잔액을 보유하지만, 상품 재고가 부족
        Long userId = businessHelper.테스트_사용자_생성();
        BigDecimal 충분한금액 = new BigDecimal("100000");
        
        businessHelper.잔액_충전(userId, 충분한금액);
        
        // 재고보다 많은 수량을 요청할 상품 선택
        ProductResponse 재고부족상품 = businessHelper.재고_부족한_상품_선택(100);
        
        // When: 재고보다 많은 수량으로 주문 시도
        ResponseEntity<CommonResponse<OrderResponse>> 주문응답 = 
            businessHelper.주문_생성_시도(userId, 재고부족상품.id(), 100);
        
        // Then: 주문이 실패하고, 잔액은 변경되지 않음
        assertAll(
            () -> assertThat(주문응답.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
            () -> {
                BalanceResponse 주문후잔액 = businessHelper.잔액_조회(userId);
                assertThat(주문후잔액.balance()).isEqualByComparingTo(충분한금액);
            }
        );
    }

    @Test
    @DisplayName("사용자가 자신의 주문 내역을 조회할 수 있다")
    void 주문_내역_조회_성공() {
        // Given: 사용자가 주문을 완료한 상태
        Long userId = businessHelper.테스트_사용자_생성();
        BigDecimal 충전금액 = new BigDecimal("1000000");
        ProductResponse 상품 = businessHelper.재고_충분한_상품_선택(1);
        
        businessHelper.잔액_충전(userId, 충전금액);
        businessHelper.잔액_충전(userId, 충전금액);
        OrderResponse 완료된주문 = businessHelper.주문_생성(userId, 상품.id(), 1);
        
        // When: 주문 내역을 조회
        List<OrderResponse> 주문내역 = businessHelper.주문_내역_조회(userId);
        
        // Then: 완료된 주문이 내역에 포함됨
        assertAll(
            () -> assertThat(주문내역).isNotEmpty(),
            () -> assertThat(주문내역).anyMatch(주문 -> 
                주문.orderId().equals(완료된주문.orderId()) && 
                주문.userId().equals(userId) &&
                "COMPLETED".equals(주문.status())
            )
        );
    }

    @Test
    @DisplayName("주문 완료 후 상품 재고가 정확히 차감된다")
    void 주문_완료_후_재고_차감_확인() {
        // Given: 충분한 잔액과 재고가 있는 상태
        Long userId = businessHelper.테스트_사용자_생성();
        BigDecimal 충전금액 = new BigDecimal("1000000");
        ProductResponse 주문전상품 = businessHelper.재고_충분한_상품_선택(2);
        int 주문수량 = 1;
        
        businessHelper.잔액_충전(userId, 충전금액);
        businessHelper.잔액_충전(userId, 충전금액);
        
        // When: 상품을 주문
        businessHelper.주문_생성(userId, 주문전상품.id(), 주문수량);
        
        // Then: 상품 재고가 정확히 차감됨
        ProductResponse 주문후상품 = businessHelper.상품_조회(주문전상품.id());
        int 예상재고 = 주문전상품.stockQuantity() - 주문수량;
        
        assertThat(주문후상품.stockQuantity()).isEqualTo(예상재고);
    }
}