package kr.hhplus.be.server.product.application;

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

import kr.hhplus.be.server.product.dto.PopularProductResponse;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.product.service.ProductService;

/**
 * GetProductsUseCase 단위 테스트
 * 
 * 테스트 전략:
 * - 단일 비즈니스 요구사항들 검증 (상품 조회, 검색, 인기상품 등)
 * - ProductService에 올바른 위임 확인
 * - ReadOnly 트랜잭션 적용 확인
 * 
 * 핵심 비즈니스 규칙:
 * - 상품 목록/상세 조회
 * - 상품 검색 (이름, 가격대)
 * - 재고 확인
 * - 인기 상품 조회
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetProductsUseCase 단위 테스트")
class GetProductsUseCaseTest {

        @Mock
        private ProductService productService;

        @InjectMocks
        private GetProductsUseCase getProductsUseCase;

        @Test
        @DisplayName("전체 상품 목록 조회 성공 - 모든 상품을 조회한다")
        void 전체상품목록조회_성공() {
                // Given
                List<ProductResponse> expectedProducts = List.of(
                                createProductResponse(1L, "노트북", "1500000", 10),
                                createProductResponse(2L, "마우스", "50000", 20));

                when(productService.getAllProducts()).thenReturn(expectedProducts);

                // When
                List<ProductResponse> responses = getProductsUseCase.executeGetAll();

                // Then
                assertThat(responses).hasSize(2);
                assertThat(responses)
                                .extracting(ProductResponse::name)
                                .containsExactly("노트북", "마우스");

                verify(productService).getAllProducts();
        }

        @Test
        @DisplayName("특정 상품 조회 성공 - 상품 ID로 상품을 조회한다")
        void 특정상품조회_성공() {
                // Given
                Long productId = 1L;
                ProductResponse expectedProduct = createProductResponse(productId, "테스트노트북", "1200000", 5);

                when(productService.getProduct(productId)).thenReturn(expectedProduct);

                // When
                ProductResponse response = getProductsUseCase.executeGet(productId);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.id()).isEqualTo(productId);
                assertThat(response.name()).isEqualTo("테스트노트북");
                assertThat(response.price()).isEqualByComparingTo(new BigDecimal("1200000"));

                verify(productService).getProduct(productId);
        }

        @Test
        @DisplayName("상품명 검색 성공 - 상품명으로 검색한다")
        void 상품명검색_성공() {
                // Given
                String searchName = "노트북";
                List<ProductResponse> expectedProducts = List.of(
                                createProductResponse(1L, "고성능 노트북", "2000000", 3),
                                createProductResponse(2L, "게이밍 노트북", "2500000", 2));

                when(productService.searchProductsByName(searchName)).thenReturn(expectedProducts);

                // When
                List<ProductResponse> responses = getProductsUseCase.executeSearch(searchName);

                // Then
                assertThat(responses).hasSize(2);
                assertThat(responses)
                                .extracting(ProductResponse::name)
                                .allMatch(name -> name.contains("노트북"));

                verify(productService).searchProductsByName(searchName);
        }

        @Test
        @DisplayName("가격 범위 검색 성공 - 최소/최대 가격으로 검색한다")
        void 가격범위검색_성공() {
                // Given
                BigDecimal minPrice = new BigDecimal("100000");
                BigDecimal maxPrice = new BigDecimal("500000");
                List<ProductResponse> expectedProducts = List.of(
                                createProductResponse(1L, "키보드", "150000", 15),
                                createProductResponse(2L, "모니터", "300000", 8));

                when(productService.getProductsByPriceRange(minPrice, maxPrice)).thenReturn(expectedProducts);

                // When
                List<ProductResponse> responses = getProductsUseCase.executeSearchByPrice(minPrice, maxPrice);

                // Then
                assertThat(responses).hasSize(2);
                assertThat(responses)
                                .extracting(ProductResponse::price)
                                .allMatch(price -> price.compareTo(minPrice) >= 0 && price.compareTo(maxPrice) <= 0);

                verify(productService).getProductsByPriceRange(minPrice, maxPrice);
        }

        @Test
        @DisplayName("재고 있는 상품 조회 성공 - 재고가 있는 상품만 조회한다")
        void 재고있는상품조회_성공() {
                // Given
                List<ProductResponse> expectedProducts = List.of(
                                createProductResponse(1L, "재고있는상품1", "100000", 5),
                                createProductResponse(2L, "재고있는상품2", "200000", 10));

                when(productService.getAvailableProducts()).thenReturn(expectedProducts);

                // When
                List<ProductResponse> responses = getProductsUseCase.executeGetAvailable();

                // Then
                assertThat(responses).hasSize(2);
                assertThat(responses)
                                .extracting(ProductResponse::stockQuantity)
                                .allMatch(stock -> stock > 0);

                verify(productService).getAvailableProducts();
        }

        @Test
        @DisplayName("인기 상품 조회 성공 - 판매량 기준 상위 N개 상품을 조회한다")
        void 인기상품조회_성공() {
                // Given
                int limit = 5;
                int days = 30;
                List<PopularProductResponse> expectedProducts = List.of(
                                new PopularProductResponse(1, 1L, "인기상품1", new BigDecimal("100000"), 50,
                                                new BigDecimal("5000000")),
                                new PopularProductResponse(2, 2L, "인기상품2", new BigDecimal("200000"), 30,
                                                new BigDecimal("6000000")));

                when(productService.getPopularProducts(limit, days)).thenReturn(expectedProducts);

                // When
                List<PopularProductResponse> responses = getProductsUseCase.executeGetPopular(limit, days);

                // Then
                assertThat(responses).hasSize(2);
                assertThat(responses)
                                .extracting(PopularProductResponse::rank)
                                .containsExactly(1, 2);
                assertThat(responses.get(0).totalSalesQuantity())
                                .isGreaterThanOrEqualTo(responses.get(1).totalSalesQuantity());

                verify(productService).getPopularProducts(limit, days);
        }

        @Test
        @DisplayName("재고 확인 성공 - 상품의 재고가 충분한지 확인한다")
        void 재고확인_성공() {
                // Given
                Long productId = 1L;
                int quantity = 3;

                when(productService.hasEnoughStock(productId, quantity)).thenReturn(true);

                // When
                boolean result = getProductsUseCase.executeStockCheck(productId, quantity);

                // Then
                assertThat(result).isTrue();

                verify(productService).hasEnoughStock(productId, quantity);
        }

        @Test
        @DisplayName("재고 확인 실패 - 상품의 재고가 부족할 때 false를 반환한다")
        void 재고확인_실패_재고부족() {
                // Given
                Long productId = 1L;
                int quantity = 10; // 재고보다 많은 수량

                when(productService.hasEnoughStock(productId, quantity)).thenReturn(false);

                // When
                boolean result = getProductsUseCase.executeStockCheck(productId, quantity);

                // Then
                assertThat(result).isFalse();

                verify(productService).hasEnoughStock(productId, quantity);
        }

        @Test
        @DisplayName("UseCase 단일 책임 검증 - 조회 관련 메서드들만 존재한다")
        void UseCase_단일책임_검증() {
                // Given: UseCase 클래스 메서드 확인
                var methods = GetProductsUseCase.class.getDeclaredMethods();
                var publicMethods = java.util.Arrays.stream(methods)
                                .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
                                .filter(method -> !method.getName().equals("equals"))
                                .filter(method -> !method.getName().equals("hashCode"))
                                .filter(method -> !method.getName().equals("toString"))
                                .map(method -> method.getName())
                                .sorted()
                                .toList();

                // Then: 조회 관련 execute 메서드들만 존재해야 함
                assertThat(publicMethods).containsExactly(
                                "executeGet", "executeGetAll", "executeGetAvailable",
                                "executeGetPopular", "executeSearch", "executeSearchByPrice", "executeStockCheck");
        }

        @Test
        @DisplayName("ReadOnly 트랜잭션 확인 - @Transactional(readOnly=true)가 적용되어 있다")
        void ReadOnly트랜잭션_확인() {
                // Given: UseCase 클래스
                Class<GetProductsUseCase> clazz = GetProductsUseCase.class;

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
                Class<GetProductsUseCase> clazz = GetProductsUseCase.class;

                // Then: @UseCase 어노테이션이 적용되어 있어야 함
                boolean hasUseCase = clazz.isAnnotationPresent(
                                kr.hhplus.be.server.common.annotation.UseCase.class);

                assertThat(hasUseCase).isTrue();
        }

        @Test
        @DisplayName("의존성 주입 확인 - ProductService만 의존한다")
        void 의존성주입_확인() {
                // Given: UseCase 필드 확인
                var fields = GetProductsUseCase.class.getDeclaredFields();
                var serviceFields = java.util.Arrays.stream(fields)
                                .filter(field -> !field.getName().contains("mockito")) // Mockito 필드 제외
                                .filter(field -> !field.getName().contains("Mock")) // Mock 필드 제외
                                .filter(field -> !field.getName().equals("log")) // Lombok @Slf4j 필드 제외
                                .filter(field -> !field.getType().equals(org.slf4j.Logger.class)) // Logger 타입 제외
                                .toList();

                // Then: ProductService 하나만 의존해야 함
                assertThat(serviceFields).hasSize(1);
                assertThat(serviceFields.get(0).getType()).isEqualTo(ProductService.class);
                assertThat(serviceFields.get(0).getName()).isEqualTo("productService");
        }

        // ==================== 테스트 헬퍼 메서드 ====================

        /**
         * 테스트용 ProductResponse 생성
         */
        private ProductResponse createProductResponse(Long id, String name, String price, Integer stock) {
                return new ProductResponse(
                                id,
                                name,
                                new BigDecimal(price),
                                stock,
                                LocalDateTime.now());
        }
}