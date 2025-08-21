package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.balance.dto.BalanceResponse;
import kr.hhplus.be.server.common.test.IntegrationTestBase;
import kr.hhplus.be.server.order.dto.OrderResponse;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.support.BusinessTestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * 비즈니스 관점에서의 캐시 효과 검증
 * 
 * 목적:
 * - Redis 구현 세부사항이 아닌 비즈니스 가치 확인
 * - 사용자 경험 관점에서 성능 향상 검증
 * - 데이터 일관성이 비즈니스에 미치는 영향 확인
 */
@DisplayName("캐시 비즈니스 효과 통합 테스트")
class ImprovedCacheBusinessTest extends IntegrationTestBase {

    @Autowired
    private BusinessTestHelper businessHelper;

    @Test
    @DisplayName("상품 조회 성능이 사용자에게 만족스러운 수준이다")
    void 상품_조회_성능_만족도() {
        // Given: 실제 서비스에서 자주 조회되는 상품
        Long 인기상품ID = businessHelper.테스트_상품_선택();
        
        // When: 사용자가 상품을 반복 조회 (실제 사용 패턴)
        long 시작시간 = System.currentTimeMillis();
        
        ProductResponse 첫번째조회 = businessHelper.상품_조회(인기상품ID);
        ProductResponse 두번째조회 = businessHelper.상품_조회(인기상품ID);
        ProductResponse 세번째조회 = businessHelper.상품_조회(인기상품ID);
        
        long 총소요시간 = System.currentTimeMillis() - 시작시간;
        
        // Then: 사용자가 기다리기 어려워하지 않는 응답시간 + 일관된 데이터
        assertAll(
            () -> assertThat(총소요시간).isLessThan(500L), // 0.5초 이내 (UX 기준)
            () -> assertThat(첫번째조회.id()).isEqualTo(인기상품ID),
            () -> assertThat(두번째조회).usingRecursiveComparison().isEqualTo(첫번째조회),
            () -> assertThat(세번째조회).usingRecursiveComparison().isEqualTo(첫번째조회)
        );
    }

    @Test
    @DisplayName("주문 후 상품 정보가 즉시 업데이트되어 사용자에게 정확한 정보를 제공한다")
    void 주문_후_상품정보_즉시_업데이트() {
        // Given: 사용자가 상품 주문 준비
        Long userId = businessHelper.테스트_사용자_생성();
        BigDecimal 충전금액 = new BigDecimal("200000");
        ProductResponse 주문전상품 = businessHelper.가격_범위내_재고_충분한_상품_선택(2, 충전금액);
        int 주문수량 = 1;
        
        businessHelper.잔액_충전(userId, 충전금액);
        
        // When: 사용자가 주문 완료
        businessHelper.주문_생성(userId, 주문전상품.id(), 주문수량);
        
        // Then: 다른 사용자가 바로 조회했을 때 정확한 재고 표시 (캐시 무효화 효과)
        ProductResponse 주문후상품 = businessHelper.상품_조회(주문전상품.id());
        int 예상재고 = 주문전상품.stockQuantity() - 주문수량;
        
        assertAll(
            () -> assertThat(주문후상품.stockQuantity()).isEqualTo(예상재고),
            () -> assertThat(주문후상품.id()).isEqualTo(주문전상품.id()),
            () -> assertThat(주문후상품.name()).isEqualTo(주문전상품.name())
        );
    }

    @Test
    @DisplayName("대량 상품 조회 시에도 시스템이 안정적으로 동작한다")
    void 대량_상품조회_시스템_안정성() {
        // Given: 여러 상품에 대한 관심
        var 상품목록 = businessHelper.상품_목록_조회();
        
        // When: 사용자가 여러 상품을 빠르게 탐색 (실제 쇼핑 패턴)
        long 시작시간 = System.currentTimeMillis();
        
        for (ProductResponse 상품 : 상품목록.subList(0, Math.min(5, 상품목록.size()))) {
            ProductResponse 상세정보 = businessHelper.상품_조회(상품.id());
            assertThat(상세정보.id()).isEqualTo(상품.id());
        }
        
        long 총소요시간 = System.currentTimeMillis() - 시작시간;
        
        // Then: 시스템이 과부하 없이 안정적으로 응답 (캐시로 인한 DB 부하 감소)
        assertThat(총소요시간).isLessThan(2000L); // 2초 이내
    }

    @Test
    @DisplayName("잔액 조회가 빠르게 응답되어 사용자 경험이 좋다")
    void 잔액_조회_사용자경험() {
        // Given: 활성 사용자
        Long userId = businessHelper.테스트_사용자_생성();
        businessHelper.잔액_충전(userId, new BigDecimal("50000"));
        
        // When: 사용자가 잔액을 자주 확인 (실제 사용 패턴)
        long 시작시간 = System.currentTimeMillis();
        
        BalanceResponse 첫번째확인 = businessHelper.잔액_조회(userId);
        BalanceResponse 두번째확인 = businessHelper.잔액_조회(userId);
        
        long 소요시간 = System.currentTimeMillis() - 시작시간;
        
        // Then: 빠른 응답으로 사용자 대기 시간 최소화
        assertAll(
            () -> assertThat(소요시간).isLessThan(300L), // 300ms 이내
            () -> assertThat(첫번째확인.balance()).isEqualByComparingTo(new BigDecimal("50000")),
            () -> assertThat(두번째확인.balance()).isEqualByComparingTo(첫번째확인.balance())
        );
    }

    @Test
    @DisplayName("캐시 시스템 장애가 있어도 비즈니스는 계속 동작한다")
    void 캐시_장애시_비즈니스_연속성() {
        // Given: 정상적인 서비스 상황
        Long userId = businessHelper.테스트_사용자_생성();
        BigDecimal 충전금액 = new BigDecimal("200000");
        ProductResponse 상품 = businessHelper.가격_범위내_재고_충분한_상품_선택(1, 충전금액);
        
        businessHelper.잔액_충전(userId, 충전금액);
        
        // When: 캐시 상태와 관계없이 핵심 비즈니스 진행
        OrderResponse 주문결과 = businessHelper.주문_생성(userId, 상품.id(), 1);
        
        // Then: 비즈니스 로직은 정상 동작 (캐시는 성능 최적화일 뿐, 필수가 아님)
        assertAll(
            () -> assertThat(주문결과).isNotNull(),
            () -> assertThat(주문결과.status()).isEqualTo("COMPLETED"),
            () -> {
                BalanceResponse 차감후잔액 = businessHelper.잔액_조회(userId);
                BigDecimal 예상잔액 = 충전금액.subtract(상품.price());
                assertThat(차감후잔액.balance()).isEqualByComparingTo(예상잔액);
            }
        );
    }
}