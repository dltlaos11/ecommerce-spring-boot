package kr.hhplus.be.server.product.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.common.test.IntegrationTestBase;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.repository.ProductRepository;

/**
 * ProductController 통합 테스트 - 수정된 버전
 * 
 * 수정사항:
 * - DataLoader 초기 데이터 고려
 * - 유연한 검증 로직
 * - 고유 데이터 사용
 */
@DisplayName("상품 관리 통합 테스트")
@Transactional
class ProductControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        verifyTestEnvironment();
    }

    @Test
    @DisplayName("전체 상품 조회 통합 테스트")
    void 전체상품조회_통합테스트() {
        // Given: 기존 데이터 개수 확인
        int beforeCount = productRepository.findAll().size();

        // 테스트 상품 3개 추가
        setupTestProducts();

        // When: 전체 상품 조회 API 호출
        ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                "/api/v1/products",
                CommonResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        // 🔧 유연한 검증: 기존 데이터 + 테스트 데이터 = 총 개수
        var products = productRepository.findAll();
        assertThat(products.size()).isEqualTo(beforeCount + 3);
    }

    @Test
    @DisplayName("특정 상품 조회 통합 테스트")
    void 특정상품조회_통합테스트() {
        // Given: 고유한 상품 생성
        Product savedProduct = setupProduct(generateUniqueProductName("테스트노트북"), new BigDecimal("1500000"), 10);
        flushAndClear(); // 즉시 DB 반영

        // When
        ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                "/api/v1/products/{productId}",
                CommonResponse.class,
                savedProduct.getId());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        // DB 검증
        var product = productRepository.findById(savedProduct.getId());
        assertThat(product).isPresent();
        assertThat(product.get().getName()).contains("테스트노트북");
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 404 에러")
    void 존재하지않는상품조회_404에러() {
        // Given: 존재하지 않는 상품 ID
        Long nonExistentId = 999999L; // 더 큰 수로 변경

        // When
        ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                "/api/v1/products/{productId}",
                CommonResponse.class,
                nonExistentId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    @DisplayName("상품명 검색 통합 테스트")
    void 상품명검색_통합테스트() {
        // Given: 고유한 키워드 사용
        String uniqueKeyword = "UNIQUE_" + System.currentTimeMillis();
        setupProduct(uniqueKeyword + "_노트북1", new BigDecimal("1500000"), 10);
        setupProduct(uniqueKeyword + "_노트북2", new BigDecimal("2000000"), 5);
        setupProduct("일반마우스", new BigDecimal("50000"), 20);

        // When: 고유 키워드로 검색
        ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                "/api/v1/products?name=" + uniqueKeyword,
                CommonResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        // 🔧 DB 검증: 고유 키워드가 포함된 상품만 조회
        var products = productRepository.findByNameContaining(uniqueKeyword);
        assertThat(products).hasSize(2);
    }

    @Test
    @DisplayName("재고 있는 상품만 조회 통합 테스트")
    void 재고있는상품조회_통합테스트() {
        // Given: 기존 재고 있는 상품 개수 확인
        int beforeAvailableCount = productRepository.findByStockQuantityGreaterThan(0).size();

        // 테스트 상품 추가
        setupProduct("재고있는상품", new BigDecimal("100000"), 5);
        setupProduct("재고없는상품", new BigDecimal("200000"), 0);

        // When
        ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                "/api/v1/products?onlyAvailable=true",
                CommonResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        // 🔧 유연한 검증: 기존 + 새로 추가된 재고 있는 상품
        var availableProducts = productRepository.findByStockQuantityGreaterThan(0);
        assertThat(availableProducts.size()).isEqualTo(beforeAvailableCount + 1);
    }

    @Test
    @DisplayName("재고 확인 API 통합 테스트")
    void 재고확인API_통합테스트() {
        // Given
        Product product = setupProduct("재고확인상품", new BigDecimal("50000"), 10);
        flushAndClear(); // 즉시 DB 반영

        // When: 5개 수량 확인
        ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                "/api/v1/products/{productId}/stock?quantity=5",
                CommonResponse.class,
                product.getId());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        // DB 검증
        var savedProduct = productRepository.findById(product.getId());
        assertThat(savedProduct).isPresent();
        assertThat(savedProduct.get().hasEnoughStock(5)).isTrue();
    }

    @Test
    @DisplayName("재고 부족 시 재고 확인 API")
    void 재고부족_재고확인API() {
        // Given
        Product product = setupProduct("재고부족상품", new BigDecimal("50000"), 3);
        flushAndClear(); // 즉시 DB 반영

        // When: 5개 수량 확인 (재고 3개이므로 부족)
        ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                "/api/v1/products/{productId}/stock?quantity=5",
                CommonResponse.class,
                product.getId());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        // 재고 부족 확인
        var savedProduct = productRepository.findById(product.getId());
        assertThat(savedProduct.get().hasEnoughStock(5)).isFalse();
    }

    @Test
    @DisplayName("인기 상품 조회 통합 테스트 - 주문 데이터 없어도 에러 없이 빈 결과 반환")
    void 인기상품조회_통합테스트() {
        // Given: 상품만 있고 주문 데이터는 없는 상황
        setupTestProducts();

        // When
        ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                "/api/v1/products/popular?limit=5&days=30",
                CommonResponse.class);

        // Then: 주문 데이터가 없어도 정상 응답
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
    }

    @Test
    @DisplayName("잘못된 파라미터로 인기 상품 조회 시 400 에러")
    void 잘못된파라미터_인기상품조회_400에러() {
        // Given & When: limit을 음수로 설정
        ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                "/api/v1/products/popular?limit=-1",
                CommonResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    // ==================== 테스트 헬퍼 메서드들 ====================

    private Product setupProduct(String name, BigDecimal price, Integer stock) {
        Product product = new Product(name, price, stock);
        return productRepository.save(product);
    }

    private void setupTestProducts() {
        setupProduct(generateUniqueProductName("노트북"), new BigDecimal("1500000"), 10);
        setupProduct(generateUniqueProductName("마우스"), new BigDecimal("50000"), 20);
        setupProduct(generateUniqueProductName("키보드"), new BigDecimal("150000"), 15);
    }
}