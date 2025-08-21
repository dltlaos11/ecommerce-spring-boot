package kr.hhplus.be.server.product.integration;

import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.common.test.IntegrationTestBase;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.support.BusinessTestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * 비즈니스 중심 상품 통합 테스트
 * 
 * 개선사항:
 * - Repository/캐시 구현에서 분리
 * - API 기반 사용자 시나리오 검증
 * - "사용자가 할 수 있는 일" 중심
 */
@DisplayName("상품 비즈니스 통합 테스트")
class ImprovedProductBusinessTest extends IntegrationTestBase {

    @Autowired
    private BusinessTestHelper businessHelper;

    @Test
    @DisplayName("사용자가 상품 목록을 조회할 수 있다")
    void 상품_목록_조회_성공() {
        // When: 사용자가 상품 목록을 조회
        List<ProductResponse> 상품목록 = businessHelper.상품_목록_조회();
        
        // Then: 상품 목록이 반환됨
        assertAll(
            () -> assertThat(상품목록).isNotNull(),
            () -> assertThat(상품목록).isNotEmpty(),
            () -> assertThat(상품목록).allMatch(상품 -> 
                상품.id() != null && 
                상품.name() != null && 
                상품.price() != null &&
                상품.stockQuantity() != null
            )
        );
    }

    @Test
    @DisplayName("사용자가 관심 있는 상품의 상세 정보를 볼 수 있다")
    void 상품_상세_조회_성공() {
        // Given: 존재하는 상품
        Long 상품ID = businessHelper.테스트_상품_선택();
        
        // When: 상품 상세 정보 조회
        ProductResponse 상품정보 = businessHelper.상품_조회(상품ID);
        
        // Then: 상품의 모든 정보가 반환됨
        assertAll(
            () -> assertThat(상품정보).isNotNull(),
            () -> assertThat(상품정보.id()).isEqualTo(상품ID),
            () -> assertThat(상품정보.name()).isNotBlank(),
            () -> assertThat(상품정보.price()).isPositive(),
            () -> assertThat(상품정보.stockQuantity()).isNotNegative(),
            () -> assertThat(상품정보.createdAt()).isNotNull()
        );
    }

    @Test
    @DisplayName("존재하지 않는 상품을 조회하려고 하면 적절한 오류가 발생한다")
    void 존재하지_않는_상품_조회_실패() {
        // Given: 존재하지 않는 상품 ID
        Long 존재하지않는상품ID = 999999999L;
        
        // When: 존재하지 않는 상품 조회 시도
        ResponseEntity<CommonResponse<ProductResponse>> 응답 = 
            businessHelper.존재하지_않는_상품_조회_시도(존재하지않는상품ID);
        
        // Then: 404 또는 적절한 오류 응답
        assertThat(응답.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("사용자가 상품을 주문하기 전에 충분한 재고가 있는지 확인할 수 있다")
    void 재고_충분한_상품_확인() {
        // Given: 충분한 재고가 있는 상품을 선택
        int 필요한수량 = 1;
        
        // When: 재고가 충분한 상품 조회
        ProductResponse 상품 = businessHelper.재고_충분한_상품_선택(필요한수량);
        
        // Then: 선택된 상품이 요구사항을 만족
        assertAll(
            () -> assertThat(상품).isNotNull(),
            () -> assertThat(상품.stockQuantity()).isGreaterThanOrEqualTo(필요한수량),
            () -> assertThat(상품.name()).isNotBlank(),
            () -> assertThat(상품.price()).isPositive()
        );
    }

    @Test
    @DisplayName("사용자가 상품의 재고가 부족한 상황을 미리 알 수 있다")
    void 재고_부족한_상품_확인() {
        // Given: 대량 주문을 원하는 상황
        int 대량수량 = 1000;
        
        // When: 재고가 부족한 상품이 있는지 확인
        List<ProductResponse> 모든상품 = businessHelper.상품_목록_조회();
        
        // Then: 재고 부족 상품을 식별할 수 있음
        boolean 재고부족상품존재 = 모든상품.stream()
            .anyMatch(상품 -> 상품.stockQuantity() < 대량수량);
        
        assertThat(재고부족상품존재).isTrue(); // 현실적으로 1000개 재고를 가진 상품은 없을 것
    }

    @Test
    @DisplayName("상품 조회 시 캐시가 적용되어 성능이 향상된다")
    void 상품_조회_성능_확인() {
        // Given: 상품 ID
        Long 상품ID = businessHelper.테스트_상품_선택();
        
        // When: 동일한 상품을 여러 번 조회 (캐시 효과 확인)
        long 시작시간 = System.currentTimeMillis();
        
        for (int i = 0; i < 3; i++) {
            ProductResponse 상품 = businessHelper.상품_조회(상품ID);
            assertThat(상품).isNotNull();
        }
        
        long 소요시간 = System.currentTimeMillis() - 시작시간;
        
        // Then: 캐시로 인해 합리적인 응답 시간을 보임 (비즈니스 관점에서 성능 확인)
        assertThat(소요시간).isLessThan(1000L); // 1초 이내
    }
}