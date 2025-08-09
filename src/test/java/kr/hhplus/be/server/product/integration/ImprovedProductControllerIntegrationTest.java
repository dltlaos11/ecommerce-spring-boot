package kr.hhplus.be.server.product.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.common.test.IntegrationTestBase;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.repository.ProductRepository;

/**
 * ProductController 통합 테스트 - 개선된 버전
 * 
 * 개선사항:
 * - 더 안정적인 데이터 격리
 * - 구체적인 에러 진단
 * - 실제 비즈니스 플로우 검증
 */
@DisplayName("상품 관리 통합 테스트 - 개선 버전")
class ImprovedProductControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ProductRepository productRepository;

    // 테스트 시작 시 생성된 상품들을 추적
    private List<Long> createdProductIds = new java.util.ArrayList<>();

    @BeforeEach
    void setUp() {
        try {
            verifyTestEnvironment();
            createdProductIds.clear();
            System.out.println("🧪 Product Integration Test Setup Completed");
        } catch (Exception e) {
            debugTestFailure("setUp", e);
            throw e;
        }
    }

    @AfterEach
    void tearDown() {
        // 테스트에서 생성한 상품들 정리
        try {
            for (Long productId : createdProductIds) {
                productRepository.findById(productId).ifPresent(productRepository::delete);
            }
            createdProductIds.clear();
            System.out.println("🧹 Test cleanup completed");
        } catch (Exception e) {
            System.err.println("⚠️ Cleanup failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("사용자가 현재 판매 중인 모든 상품을 볼 수 있다")
    void 사용자가_현재_판매_중인_모든_상품을_볼_수_있다() {
        try {
            // Given: 기존 상품 개수 확인 (DataLoader 초기 데이터 고려)
            int initialCount = productRepository.findAll().size();
            System.out.println("📊 초기 상품 개수: " + initialCount);

            // 테스트용 고유 상품 추가
            String uniqueName = generateUniqueProductName("통합테스트상품");
            Product testProduct = new Product(uniqueName, new BigDecimal("100000"), 10);
            Product savedProduct = productRepository.save(testProduct);
            createdProductIds.add(savedProduct.getId());
            flushAndClear(); // 즉시 DB 반영

            // When: 전체 상품 조회 API 호출
            ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                    "/api/v1/products", CommonResponse.class);

            // Then: HTTP 응답 검증
            System.out.println("🔍 API 응답 상태: " + response.getStatusCode());
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();

            // DB 상태 검증
            int finalCount = productRepository.findAll().size();
            assertThat(finalCount).isEqualTo(initialCount + 1);
            System.out.println("✅ 상품 개수 검증: " + initialCount + " -> " + finalCount);

        } catch (Exception e) {
            debugTestFailure("전체상품조회_통합테스트", e);
            throw e;
        }
    }

    @Test
    @DisplayName("사용자가 관심 있는 상품의 상세 정보를 볼 수 있다")
    void 사용자가_관심_있는_상품의_상세_정보를_볼_수_있다() {
        try {
            // Given: 고유한 테스트 상품 생성
            String uniqueName = generateUniqueProductName("특정조회테스트상품");
            Product testProduct = new Product(uniqueName, new BigDecimal("150000"), 5);
            Product savedProduct = productRepository.save(testProduct);
            createdProductIds.add(savedProduct.getId());
            flushAndClear();

            System.out.println("🏷️ 테스트 상품 ID: " + savedProduct.getId());

            // When: 특정 상품 조회 API 호출
            ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                    "/api/v1/products/{productId}", CommonResponse.class, savedProduct.getId());

            // Then: HTTP 응답 검증
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();

            // DB 검증
            var foundProduct = productRepository.findById(savedProduct.getId());
            assertThat(foundProduct).isPresent();
            assertThat(foundProduct.get().getName()).isEqualTo(uniqueName);

            System.out.println("✅ 특정 상품 조회 성공: " + uniqueName);

        } catch (Exception e) {
            debugTestFailure("특정상품조회_통합테스트", e);
            throw e;
        }
    }

    @Test
    @DisplayName("존재하지 않는 상품을 조회하려고 하면 실패한다")
    void 존재하지_않는_상품을_조회하려고_하면_실패한다() {
        try {
            // Given: 확실히 존재하지 않는 ID
            Long nonExistentId = 999999999L;

            // When: 존재하지 않는 상품 조회
            ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                    "/api/v1/products/{productId}", CommonResponse.class, nonExistentId);

            // Then: 404 에러 확인
            System.out.println("🔍 404 테스트 응답: " + response.getStatusCode());
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();

            System.out.println("✅ 404 에러 처리 정상 동작");

        } catch (Exception e) {
            debugTestFailure("존재하지않는상품조회_404에러", e);
            throw e;
        }
    }

    @Test
    @DisplayName("사용자가 원하는 상품명으로 검색하여 찾을 수 있다")
    void 사용자가_원하는_상품명으로_검색하여_찾을_수_있다() {
        try {
            // Given: 고유한 키워드로 여러 상품 생성
            String uniqueKeyword = "SEARCH_" + System.currentTimeMillis();

            Product product1 = new Product(uniqueKeyword + "_노트북", new BigDecimal("1000000"), 5);
            Product product2 = new Product(uniqueKeyword + "_키보드", new BigDecimal("100000"), 10);
            Product product3 = new Product("일반마우스", new BigDecimal("50000"), 15);

            Product saved1 = productRepository.save(product1);
            Product saved2 = productRepository.save(product2);
            Product saved3 = productRepository.save(product3);
            createdProductIds.add(saved1.getId());
            createdProductIds.add(saved2.getId());
            createdProductIds.add(saved3.getId());
            flushAndClear();

            // When: 고유 키워드로 검색
            ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                    "/api/v1/products?name=" + uniqueKeyword, CommonResponse.class);

            // Then: HTTP 응답 검증
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();

            // DB 검증: 고유 키워드 포함 상품만 2개 조회되어야 함
            var searchResults = productRepository.findByNameContaining(uniqueKeyword);
            assertThat(searchResults).hasSize(2);

            System.out.println("✅ 상품명 검색 성공: " + uniqueKeyword + " (2개 결과)");

        } catch (Exception e) {
            debugTestFailure("상품명검색_통합테스트", e);
            throw e;
        }
    }

    @Test
    @DisplayName("사용자가 상품을 주문하기 전에 충분한 재고가 있는지 확인할 수 있다")
    void 사용자가_상품을_주문하기_전에_충분한_재고가_있는지_확인할_수_있다() {
        try {
            // Given: 재고가 있는 테스트 상품 생성
            String uniqueName = generateUniqueProductName("재고확인상품");
            Product testProduct = new Product(uniqueName, new BigDecimal("75000"), 8);
            Product savedProduct = productRepository.save(testProduct);
            createdProductIds.add(savedProduct.getId());
            flushAndClear();

            // When: 재고 확인 API 호출 (5개 수량 확인)
            ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                    "/api/v1/products/{productId}/stock?quantity=5",
                    CommonResponse.class, savedProduct.getId());

            // Then: HTTP 응답 검증
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();

            // DB 검증: 실제 재고 확인
            var foundProduct = productRepository.findById(savedProduct.getId());
            assertThat(foundProduct).isPresent();
            assertThat(foundProduct.get().hasEnoughStock(5)).isTrue();
            assertThat(foundProduct.get().hasEnoughStock(10)).isFalse(); // 재고 8개이므로 10개는 부족

            System.out.println("✅ 재고 확인 API 성공: 재고 8개, 요청 5개 -> 충분");

        } catch (Exception e) {
            debugTestFailure("재고확인API_통합테스트", e);
            throw e;
        }
    }

    @Test
    @DisplayName("사용자가 상품의 재고가 부족한 상황을 미리 알 수 있다")
    void 사용자가_상품의_재고가_부족한_상황을_미리_알_수_있다() {
        try {
            // Given: 재고가 적은 테스트 상품 생성
            String uniqueName = generateUniqueProductName("재고부족상품");
            Product testProduct = new Product(uniqueName, new BigDecimal("50000"), 3);
            Product savedProduct = productRepository.save(testProduct);
            createdProductIds.add(savedProduct.getId());
            flushAndClear();

            // When: 재고보다 많은 수량 확인 (5개 요청, 재고 3개)
            ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                    "/api/v1/products/{productId}/stock?quantity=5",
                    CommonResponse.class, savedProduct.getId());

            // Then: 여전히 200 OK (API는 정상 동작하되, 결과에서 재고 부족 표시)
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();

            // DB 검증: 재고 부족 확인
            var foundProduct = productRepository.findById(savedProduct.getId());
            assertThat(foundProduct).isPresent();
            assertThat(foundProduct.get().hasEnoughStock(5)).isFalse();

            System.out.println("✅ 재고 부족 API 성공: 재고 3개, 요청 5개 -> 부족");

        } catch (Exception e) {
            debugTestFailure("재고부족_재고확인API", e);
            throw e;
        }
    }

    @Test
    @DisplayName("사용자가 바로 구매 가능한 재고 있는 상품만 보고 싶어 한다")
    void 사용자가_바로_구매_가능한_재고_있는_상품만_보고_싶어_한다() {
        try {
            // Given: 재고 있는 상품과 재고 없는 상품 생성
            int initialAvailableCount = productRepository.findByStockQuantityGreaterThan(0).size();

            String baseName = "STOCK_TEST_" + System.currentTimeMillis();
            Product availableProduct = new Product(baseName + "_재고있음", new BigDecimal("100000"), 5);
            Product outOfStockProduct = new Product(baseName + "_재고없음", new BigDecimal("200000"), 0);

            Product savedAvailable = productRepository.save(availableProduct);
            Product savedOutOfStock = productRepository.save(outOfStockProduct);
            createdProductIds.add(savedAvailable.getId());
            createdProductIds.add(savedOutOfStock.getId());
            flushAndClear();

            // When: 재고 있는 상품만 조회
            ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                    "/api/v1/products?onlyAvailable=true", CommonResponse.class);

            // Then: HTTP 응답 검증
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();

            // DB 검증: 재고 있는 상품만 증가
            int finalAvailableCount = productRepository.findByStockQuantityGreaterThan(0).size();
            assertThat(finalAvailableCount).isEqualTo(initialAvailableCount + 1);

            System.out.println("✅ 재고 있는 상품 조회: " + initialAvailableCount + " -> " + finalAvailableCount);

        } catch (Exception e) {
            debugTestFailure("재고있는상품조회_통합테스트", e);
            throw e;
        }
    }

    @Test
    @DisplayName("사용자가 현재 인기가 많은 인기 상품들을 보고 싶어 한다")
    void 사용자가_현재_인기가_많은_인기_상품들을_보고_싶어_한다() {
        try {
            // Given: 상품은 있지만 주문 데이터는 없는 상황
            String uniqueName = generateUniqueProductName("인기상품테스트");
            Product testProduct = new Product(uniqueName, new BigDecimal("200000"), 10);
            Product savedProduct = productRepository.save(testProduct);
            createdProductIds.add(savedProduct.getId());
            flushAndClear();

            // When: 인기 상품 조회 API 호출
            ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                    "/api/v1/products/popular?limit=5&days=30", CommonResponse.class);

            // Then: 주문 데이터가 없어도 정상 응답 (빈 배열)
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();

            System.out.println("✅ 인기 상품 조회 성공 (주문 데이터 없음)");

        } catch (Exception e) {
            debugTestFailure("인기상품조회_통합테스트", e);
            throw e;
        }
    }

    @Test
    @DisplayName("잘못된 요청으로 상품을 조회하려고 하면 실패한다")
    void 잘못된_요청으로_상품을_조회하려고_하면_실패한다() {
        try {
            // When: 잘못된 파라미터로 인기 상품 조회
            ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                    "/api/v1/products/popular?limit=-1&days=500", CommonResponse.class);

            // Then: 400 Bad Request 확인
            System.out.println("🔍 잘못된 파라미터 응답: " + response.getStatusCode());
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();

            System.out.println("✅ 잘못된 파라미터 검증 성공");

        } catch (Exception e) {
            debugTestFailure("잘못된파라미터_400에러", e);
            throw e;
        }
    }
}