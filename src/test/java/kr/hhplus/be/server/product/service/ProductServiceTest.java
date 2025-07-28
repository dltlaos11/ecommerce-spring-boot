package kr.hhplus.be.server.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.repository.OrderItemRepository;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.dto.PopularProductResponse;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.product.exception.InsufficientStockException;
import kr.hhplus.be.server.product.exception.ProductNotFoundException;
import kr.hhplus.be.server.product.repository.ProductRepository;

/**
 * ProductService 단위 테스트
 * 
 * ✨ 테스트 전략:
 * - Mock을 활용한 외부 의존성 격리
 * - Given-When-Then 패턴으로 명확한 테스트 구조
 * - 성공/실패 케이스 모두 검증
 * - 비즈니스 로직에만 집중
 * 
 * 🎯 테스트 범위:
 * - 상품 조회 (성공/실패)
 * - 재고 관리 (차감/복구)
 * - 검색 기능
 * - DTO 변환 로직
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 단위 테스트")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository; // 가짜 Repository

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ProductService productService; // 진짜 Service (Mock이 주입됨)

    @Test
    @DisplayName("상품 조회 성공 - 존재하는 상품ID로 조회할 때 상품 정보를 반환한다")
    void 상품조회_성공() {
        // Given: 테스트 데이터 준비
        Long productId = 1L;
        Product mockProduct = createTestProduct(productId, "테스트 노트북", "1000000", 5);

        // Stub 설정: productRepository.findById(1L) 호출 시 mockProduct 반환
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));

        // When: 실제 테스트할 메서드 실행
        ProductResponse response = productService.getProduct(productId);

        // Then: 결과 검증
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo("테스트 노트북");
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("1000000"));
        assertThat(response.stockQuantity()).isEqualTo(5);

        // Mock 호출 검증: findById가 정확히 1번 호출되었는지 확인
        verify(productRepository).findById(productId);
    }

    @Test
    @DisplayName("상품 조회 실패 - 존재하지 않는 상품ID로 조회할 때 예외가 발생한다")
    void 상품조회_실패_상품없음() {
        // Given: 존재하지 않는 상품 시뮬레이션
        Long nonExistentProductId = 999L;
        when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

        // When & Then: 예외 발생 검증
        assertThatThrownBy(() -> productService.getProduct(nonExistentProductId))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository).findById(nonExistentProductId);
    }

    @Test
    @DisplayName("전체 상품 조회 성공 - 모든 상품 목록을 반환한다")
    void 전체상품조회_성공() {
        // Given: 여러 상품 목록 준비
        List<Product> mockProducts = List.of(
                createTestProduct(1L, "노트북", "1000000", 10),
                createTestProduct(2L, "마우스", "50000", 20),
                createTestProduct(3L, "키보드", "100000", 15));

        when(productRepository.findAll()).thenReturn(mockProducts);

        // When
        List<ProductResponse> responses = productService.getAllProducts();

        // Then
        assertThat(responses).hasSize(3);
        assertThat(responses)
                .extracting(ProductResponse::name) // 상품명만 추출해서 검증
                .containsExactly("노트북", "마우스", "키보드");

        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("재고 확인 성공 - 충분한 재고가 있을 때 true를 반환한다")
    void 재고확인_성공_충분한재고() {
        // Given
        Long productId = 1L;
        Product product = createTestProduct(productId, "테스트 상품", "10000", 10);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // When: 5개 주문 시도 (재고 10개이므로 충분)
        boolean hasEnough = productService.hasEnoughStock(productId, 5);

        // Then
        assertThat(hasEnough).isTrue();
        verify(productRepository).findById(productId);
    }

    @Test
    @DisplayName("재고 확인 실패 - 재고가 부족할 때 false를 반환한다")
    void 재고확인_실패_재고부족() {
        // Given
        Long productId = 1L;
        Product product = createTestProduct(productId, "테스트 상품", "10000", 3);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // When: 5개 주문 시도 (재고 3개이므로 부족)
        boolean hasEnough = productService.hasEnoughStock(productId, 5);

        // Then
        assertThat(hasEnough).isFalse();
        verify(productRepository).findById(productId);
    }

    @Test
    @DisplayName("재고 차감 성공 - 충분한 재고가 있을 때 재고가 정상적으로 차감된다")
    void 재고차감_성공() {
        // Given
        Long productId = 1L;
        Product product = createTestProduct(productId, "테스트 상품", "10000", 10);

        // findByIdForUpdate 호출 시 상품 반환 (비관적 락 시뮬레이션)
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));

        // save 호출 시 저장된 상품 그대로 반환
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When: 3개 차감
        productService.reduceStock(productId, 3);

        // Then: 재고가 7개로 줄어들었는지 확인
        assertThat(product.getStockQuantity()).isEqualTo(7);

        // Mock 호출 검증
        verify(productRepository).findByIdForUpdate(productId);
        verify(productRepository).save(product);

        // 호출 순서 검증
        var inOrder = inOrder(productRepository);
        inOrder.verify(productRepository).findByIdForUpdate(productId);
        inOrder.verify(productRepository).save(product);
    }

    @Test
    @DisplayName("재고 차감 실패 - 재고보다 많은 수량을 차감하려 할 때 예외가 발생한다")
    void 재고차감_실패_재고부족() {
        // Given: 재고가 5개인 상품
        Long productId = 1L;
        Product product = createTestProduct(productId, "테스트 상품", "10000", 5);

        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));

        // When & Then: 10개 차감 시도 → 예외 발생
        assertThatThrownBy(() -> productService.reduceStock(productId, 10))
                .isInstanceOf(InsufficientStockException.class);

        // 재고는 변하지 않아야 함
        assertThat(product.getStockQuantity()).isEqualTo(5);

        // save는 호출되지 않아야 함 (예외 발생으로 중단됨)
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("재고 복구 성공 - 주문 취소 시 재고가 정상적으로 복구된다")
    void 재고복구_성공() {
        // Given
        Long productId = 1L;
        Product product = createTestProduct(productId, "테스트 상품", "10000", 7);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When: 3개 복구
        productService.restoreStock(productId, 3);

        // Then: 재고가 10개로 늘어났는지 확인
        assertThat(product.getStockQuantity()).isEqualTo(10);

        verify(productRepository).findById(productId);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("상품명 검색 성공 - 검색어가 포함된 상품들을 반환한다")
    void 상품명검색_성공() {
        // Given
        String searchKeyword = "노트북";
        List<Product> mockProducts = List.of(
                createTestProduct(1L, "고성능 노트북", "1500000", 10),
                createTestProduct(2L, "게이밍 노트북", "2000000", 5));

        when(productRepository.findByNameContaining(searchKeyword)).thenReturn(mockProducts);

        // When
        List<ProductResponse> responses = productService.searchProductsByName(searchKeyword);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(ProductResponse::name)
                .allMatch(name -> name.contains("노트북"));

        verify(productRepository).findByNameContaining(searchKeyword);
    }

    @Test
    @DisplayName("상품명 검색 - 빈 검색어로 조회 시 전체 상품을 반환한다")
    void 상품명검색_빈검색어_전체상품반환() {
        // Given
        List<Product> allProducts = List.of(
                createTestProduct(1L, "노트북", "1000000", 10),
                createTestProduct(2L, "마우스", "50000", 20));

        when(productRepository.findAll()).thenReturn(allProducts);

        // When: 빈 문자열로 검색
        List<ProductResponse> responses = productService.searchProductsByName("");

        // Then: 전체 상품 반환
        assertThat(responses).hasSize(2);
        verify(productRepository).findAll();
        verify(productRepository, never()).findByNameContaining(anyString());
    }

    @Test
    @DisplayName("가격 범위 검색 성공 - 지정된 가격 범위의 상품들을 반환한다")
    void 가격범위검색_성공() {
        // Given
        BigDecimal minPrice = new BigDecimal("100000");
        BigDecimal maxPrice = new BigDecimal("500000");

        List<Product> mockProducts = List.of(
                createTestProduct(1L, "키보드", "150000", 10),
                createTestProduct(2L, "모니터", "300000", 5));

        when(productRepository.findByPriceBetween(minPrice, maxPrice)).thenReturn(mockProducts);

        // When
        List<ProductResponse> responses = productService.getProductsByPriceRange(minPrice, maxPrice);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(ProductResponse::price)
                .allMatch(price -> price.compareTo(minPrice) >= 0 && price.compareTo(maxPrice) <= 0);

        verify(productRepository).findByPriceBetween(minPrice, maxPrice);
    }

    @Test
    @DisplayName("재고 있는 상품 조회 성공 - 재고가 0보다 큰 상품들만 반환한다")
    void 재고있는상품조회_성공() {
        // Given
        List<Product> availableProducts = List.of(
                createTestProduct(1L, "노트북", "1000000", 5),
                createTestProduct(2L, "마우스", "50000", 10)
        // 재고가 0인 상품은 제외됨
        );

        when(productRepository.findByStockQuantityGreaterThan(0)).thenReturn(availableProducts);

        // When
        List<ProductResponse> responses = productService.getAvailableProducts();

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(ProductResponse::stockQuantity)
                .allMatch(stock -> stock > 0);

        verify(productRepository).findByStockQuantityGreaterThan(0);
    }

    @Test
    @DisplayName("상품 생성 성공 - 유효한 데이터로 상품을 생성한다")
    void 상품생성_성공() {
        // Given
        String name = "새로운 상품";
        BigDecimal price = new BigDecimal("50000");
        Integer stockQuantity = 100;

        Product savedProduct = createTestProduct(1L, name, price.toString(), stockQuantity);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // When
        ProductResponse response = productService.createProduct(name, price, stockQuantity);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo(name);
        assertThat(response.price()).isEqualByComparingTo(price);
        assertThat(response.stockQuantity()).isEqualTo(stockQuantity);

        // save가 호출되었는지 검증
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("상품 수정 성공 - 기존 상품의 정보를 수정한다")
    void 상품수정_성공() {
        // Given
        Long productId = 1L;
        Product existingProduct = createTestProduct(productId, "기존 상품", "100000", 10);

        String newName = "수정된 상품";
        BigDecimal newPrice = new BigDecimal("150000");

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

        // When
        ProductResponse response = productService.updateProduct(productId, newName, newPrice);

        // Then
        assertThat(response.name()).isEqualTo(newName);
        assertThat(response.price()).isEqualByComparingTo(newPrice);

        // 도메인 객체의 상태도 변경되었는지 확인
        assertThat(existingProduct.getName()).isEqualTo(newName);
        assertThat(existingProduct.getPrice()).isEqualByComparingTo(newPrice);

        verify(productRepository).findById(productId);
        verify(productRepository).save(existingProduct);
    }

    @Test
    @DisplayName("상품 삭제 성공 - 기존 상품을 삭제한다")
    void 상품삭제_성공() {
        // Given
        Long productId = 1L;
        Product existingProduct = createTestProduct(productId, "삭제할 상품", "100000", 10);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));

        // When
        productService.deleteProduct(productId);

        // Then
        verify(productRepository).findById(productId);
        verify(productRepository).delete(existingProduct);
    }

    @Test
    @DisplayName("재고 확인 실패 - 존재하지 않는 상품ID로 확인 시 예외가 발생한다")
    void 재고확인_실패_상품없음() {
        // Given
        Long nonExistentProductId = 999L;
        when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.hasEnoughStock(nonExistentProductId, 5))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository).findById(nonExistentProductId);
    }

    @Test
    @DisplayName("인기 상품 조회 성공 - 판매량 기준으로 정렬된다")
    void 인기상품조회_성공() {
        // Given
        List<OrderItem> mockOrderItems = createMockOrderItemsForStats();
        when(orderItemRepository.findAll()).thenReturn(mockOrderItems);

        // When
        List<PopularProductResponse> results = productService.getPopularProducts(3, 30);

        // Then
        assertThat(results).hasSize(3);

        // 순위 확인
        assertThat(results.get(0).rank()).isEqualTo(1);
        assertThat(results.get(1).rank()).isEqualTo(2);
        assertThat(results.get(2).rank()).isEqualTo(3);

        // 판매량 내림차순 확인
        assertThat(results.get(0).totalSalesQuantity())
                .isGreaterThanOrEqualTo(results.get(1).totalSalesQuantity());
        assertThat(results.get(1).totalSalesQuantity())
                .isGreaterThanOrEqualTo(results.get(2).totalSalesQuantity());

        verify(orderItemRepository).findAll();
    }

    @Test
    @DisplayName("인기 상품 조회 - 주문 데이터가 없을 때 빈 리스트를 반환한다")
    void 인기상품조회_주문데이터없음() {
        // Given
        when(orderItemRepository.findAll()).thenReturn(List.of());

        // When
        List<PopularProductResponse> results = productService.getPopularProducts(5, 30);

        // Then
        assertThat(results).isEmpty();
        verify(orderItemRepository).findAll();
    }

    @Test
    @DisplayName("인기 상품 조회 - 지정된 개수만큼만 반환한다")
    void 인기상품조회_개수제한() {
        // Given
        List<OrderItem> mockOrderItems = createMockOrderItemsForStats(); // 5개 상품
        when(orderItemRepository.findAll()).thenReturn(mockOrderItems);

        // When: 3개만 요청
        List<PopularProductResponse> results = productService.getPopularProducts(3, 30);

        // Then: 3개만 반환
        assertThat(results).hasSize(3);
    }

    /**
     * 테스트용 Product 객체 생성 헬퍼 메서드
     * 
     * 🎯 목적:
     * - 테스트 데이터 생성의 일관성 보장
     * - 코드 중복 제거
     * - 유지보수성 향상
     */
    private Product createTestProduct(Long id, String name, String price, Integer stock) {
        Product product = new Product(name, new BigDecimal(price), stock);
        product.setId(id);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return product;
    }

    // 테스트용 OrderItem 생성 헬퍼 메서드
    private List<OrderItem> createMockOrderItemsForStats() {
        LocalDateTime now = LocalDateTime.now();

        return List.of(
                // 상품 1: 총 판매량 10개
                createMockOrderItem(1L, "노트북", new BigDecimal("1000000"), 5, now.minusDays(1)),
                createMockOrderItem(1L, "노트북", new BigDecimal("1000000"), 5, now.minusDays(2)),

                // 상품 2: 총 판매량 15개 (1위)
                createMockOrderItem(2L, "마우스", new BigDecimal("50000"), 10, now.minusDays(1)),
                createMockOrderItem(2L, "마우스", new BigDecimal("50000"), 5, now.minusDays(3)),

                // 상품 3: 총 판매량 8개
                createMockOrderItem(3L, "키보드", new BigDecimal("150000"), 8, now.minusDays(1)),

                // 상품 4: 총 판매량 12개 (2위)
                createMockOrderItem(4L, "모니터", new BigDecimal("300000"), 12, now.minusDays(2)),

                // 상품 5: 총 판매량 6개
                createMockOrderItem(5L, "스피커", new BigDecimal("200000"), 6, now.minusDays(1)));
    }

    private OrderItem createMockOrderItem(Long productId, String productName,
            BigDecimal price, Integer quantity,
            LocalDateTime createdAt) {
        OrderItem item = new OrderItem(productId, productName, price, quantity);
        item.setId(System.nanoTime()); // 유니크한 ID
        item.setCreatedAt(createdAt);
        return item;
    }
}