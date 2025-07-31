package kr.hhplus.be.server.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.balance.domain.UserBalance;
import kr.hhplus.be.server.balance.repository.UserBalanceRepository;
import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.common.test.IntegrationTestBase;
import kr.hhplus.be.server.order.dto.CreateOrderRequest;
import kr.hhplus.be.server.order.dto.OrderItemRequest;
import kr.hhplus.be.server.order.repository.OrderRepository;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.repository.ProductRepository;

/**
 * OrderController 통합 테스트
 * 
 * 핵심 비즈니스 플로우 검증:
 * - 주문 생성 전체 플로우 (재고확인 → 잔액결제 → 재고차감 → 주문생성)
 * - 재고 부족, 잔액 부족 등 예외 상황
 * - 복합 도메인 워크플로우 검증
 */
@DisplayName("주문 관리 통합 테스트")
@Transactional
class OrderControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        verifyTestEnvironment();
        // @Sql 어노테이션으로 자동 정리되므로 별도 정리 불필요
    }

    @Test
    @DisplayName("주문 생성 통합 테스트 - 전체 플로우가 정상 동작한다")
    void 주문생성_통합테스트() {
        // Given: 테스트 데이터 준비 (고유 ID 사용)
        Long userId = generateUniqueUserId();
        setupUserBalance(userId, new BigDecimal("100000"));
        Product product = setupProduct(generateUniqueProductName("테스트노트북"), new BigDecimal("50000"), 10);

        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                List.of(new OrderItemRequest(product.getId(), 2)),
                null // 쿠폰 없음
        );

        // When: 주문 생성 API 호출
        ResponseEntity<CommonResponse> response = restTemplate.postForEntity(
                "/api/v1/orders",
                request,
                CommonResponse.class);

        // Then: HTTP 응답 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().isSuccess()).isTrue();

        // DB 상태 검증
        // 1. 잔액 차감 확인
        var updatedBalance = userBalanceRepository.findByUserId(userId);
        assertThat(updatedBalance).isPresent();
        assertThat(updatedBalance.get().getBalance()).isEqualByComparingTo(BigDecimal.ZERO); // 100000 - (50000 * 2)

        // 2. 재고 차감 확인
        var updatedProduct = productRepository.findById(product.getId());
        assertThat(updatedProduct).isPresent();
        assertThat(updatedProduct.get().getStockQuantity()).isEqualTo(8); // 10 - 2

        // 3. 주문 생성 확인
        var orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getFinalAmount()).isEqualByComparingTo(new BigDecimal("100000"));
    }

    @Test
    @DisplayName("주문 생성 실패 - 재고 부족 시 400 에러 발생")
    void 주문생성_실패_재고부족() {
        // Given
        Long userId = generateUniqueUserId();
        setupUserBalance(userId, new BigDecimal("200000"));
        Product product = setupProduct(generateUniqueProductName("재고부족상품"), new BigDecimal("50000"), 1); // 재고 1개만

        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                List.of(new OrderItemRequest(product.getId(), 5)), // 5개 주문 시도
                null);

        // When
        ResponseEntity<CommonResponse> response = restTemplate.postForEntity(
                "/api/v1/orders",
                request,
                CommonResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();

        // 잔액과 재고가 변하지 않았는지 확인
        var balance = userBalanceRepository.findByUserId(userId);
        assertThat(balance.get().getBalance()).isEqualByComparingTo(new BigDecimal("200000"));

        var productState = productRepository.findById(product.getId());
        assertThat(productState.get().getStockQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("주문 생성 실패 - 잔액 부족 시 400 에러 발생")
    void 주문생성_실패_잔액부족() {
        // Given
        Long userId = generateUniqueUserId();
        setupUserBalance(userId, new BigDecimal("30000")); // 부족한 잔액
        Product product = setupProduct(generateUniqueProductName("고가상품"), new BigDecimal("50000"), 10);

        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                List.of(new OrderItemRequest(product.getId(), 1)), // 50000원 상품
                null);

        // When
        ResponseEntity<CommonResponse> response = restTemplate.postForEntity(
                "/api/v1/orders",
                request,
                CommonResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();

        // 상태 변화 없음 확인
        var balance = userBalanceRepository.findByUserId(userId);
        assertThat(balance.get().getBalance()).isEqualByComparingTo(new BigDecimal("30000"));

        var productState = productRepository.findById(product.getId());
        assertThat(productState.get().getStockQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("사용자 주문 목록 조회 통합 테스트")
    void 사용자주문목록조회_통합테스트() {
        // Given: 사용자가 여러 주문을 했다고 가정
        Long userId = generateUniqueUserId();
        setupUserBalance(userId, new BigDecimal("200000"));
        Product product1 = setupProduct(generateUniqueProductName("상품1"), new BigDecimal("30000"), 10);
        Product product2 = setupProduct(generateUniqueProductName("상품2"), new BigDecimal("40000"), 10);

        // 첫 번째 주문
        CreateOrderRequest request1 = new CreateOrderRequest(
                userId, List.of(new OrderItemRequest(product1.getId(), 1)), null);
        restTemplate.postForEntity("/api/v1/orders", request1, CommonResponse.class);

        // 두 번째 주문
        CreateOrderRequest request2 = new CreateOrderRequest(
                userId, List.of(new OrderItemRequest(product2.getId(), 1)), null);
        restTemplate.postForEntity("/api/v1/orders", request2, CommonResponse.class);

        // When: 주문 목록 조회
        ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                "/api/v1/orders/users/{userId}",
                CommonResponse.class,
                userId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        // DB 검증
        var orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(orders).hasSize(2);
    }

    @Test
    @DisplayName("잘못된 주문 요청 시 400 에러 발생")
    void 잘못된주문요청_400에러() {
        // Given: 필수 필드 누락
        CreateOrderRequest invalidRequest = new CreateOrderRequest(
                null, // userId 누락
                List.of(new OrderItemRequest(1L, 1)),
                null);

        // When
        ResponseEntity<CommonResponse> response = restTemplate.postForEntity(
                "/api/v1/orders",
                invalidRequest,
                CommonResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    // ==================== 테스트 헬퍼 메서드들 ====================

    private void setupUserBalance(Long userId, BigDecimal amount) {
        UserBalance balance = new UserBalance(userId);
        balance.charge(amount);
        userBalanceRepository.save(balance);
    }

    private Product setupProduct(String name, BigDecimal price, Integer stock) {
        Product product = new Product(name, price, stock);
        // 테스트에서는 ID를 직접 설정할 수 없으므로 저장 후 반환받은 객체 사용
        return productRepository.save(product);
    }
}