package kr.hhplus.be.server.order.integration;

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
 * OrderController 통합 테스트 - 수정된 버전
 * 
 * 수정사항:
 * - 테스트 격리 개선
 * - 에러 처리 강화
 * - 데이터 검증 로직 수정
 */
@DisplayName("주문 관리 통합 테스트")
class OrderControllerIntegrationTest extends IntegrationTestBase {

        @Autowired
        private UserBalanceRepository userBalanceRepository;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private OrderRepository orderRepository;

        // 테스트에서 생성한 데이터들을 추적
        private List<Long> createdUserIds = new java.util.ArrayList<>();
        private List<Long> createdProductIds = new java.util.ArrayList<>();
        private List<Long> createdOrderIds = new java.util.ArrayList<>();

        @BeforeEach
        void setUp() {
                try {
                        verifyTestEnvironment();
                        createdUserIds.clear();
                        createdProductIds.clear();
                        createdOrderIds.clear();
                        System.out.println("🧪 Order Integration Test Setup Completed");
                } catch (Exception e) {
                        debugTestFailure("setUp", e);
                        throw e;
                }
        }

        @AfterEach
        void tearDown() {
                // 테스트에서 생성한 데이터들 정리
                try {
                        // 주문 삭제
                        for (Long orderId : createdOrderIds) {
                                orderRepository.findById(orderId).ifPresent(orderRepository::delete);
                        }
                        
                        // 상품 삭제
                        for (Long productId : createdProductIds) {
                                productRepository.findById(productId).ifPresent(productRepository::delete);
                        }
                        
                        // 사용자 잔액 삭제
                        for (Long userId : createdUserIds) {
                                userBalanceRepository.findByUserId(userId).ifPresent(userBalanceRepository::delete);
                        }
                        
                        createdUserIds.clear();
                        createdProductIds.clear();
                        createdOrderIds.clear();
                        System.out.println("🧹 Order test cleanup completed");
                } catch (Exception e) {
                        System.err.println("⚠️ Order cleanup failed: " + e.getMessage());
                }
        }

        @Test
        @DisplayName("사용자가 상품을 선택하여 주문을 완료할 수 있다")
        void 사용자가_상품을_선택하여_주문을_완료할_수_있다() {
                // Given: 테스트 데이터 준비
                Long userId = generateUniqueUserId();
                BigDecimal chargeAmount = new BigDecimal("100000");
                BigDecimal productPrice = new BigDecimal("50000");

                // 잔액 충전
                UserBalance userBalance = new UserBalance(userId);
                userBalance.charge(chargeAmount);
                userBalanceRepository.save(userBalance);
                createdUserIds.add(userId);

                // 상품 생성
                Product product = new Product(generateUniqueProductName("테스트노트북"), productPrice, 10);
                Product savedProduct = productRepository.save(product);
                createdProductIds.add(savedProduct.getId());
                
                flushAndClear();

                CreateOrderRequest request = new CreateOrderRequest(
                                userId,
                                List.of(new OrderItemRequest(savedProduct.getId(), 2)),
                                null // 쿠폰 없음
                );

                // When: 주문 생성 API 호출
                ResponseEntity<CommonResponse> response = restTemplate.postForEntity(
                                "/api/v1/orders",
                                request,
                                CommonResponse.class);

                // Then: HTTP 응답 검증
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isTrue();

                // DB 상태 검증
                // 1. 잔액 차감 확인 (100000 - 50000*2 = 0)
                var updatedBalance = userBalanceRepository.findByUserId(userId);
                assertThat(updatedBalance).isPresent();
                assertThat(updatedBalance.get().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);

                // 2. 재고 차감 확인 (10 - 2 = 8)
                var updatedProduct = productRepository.findById(savedProduct.getId());
                assertThat(updatedProduct).isPresent();
                assertThat(updatedProduct.get().getStockQuantity()).isEqualTo(8);

                // 3. 주문 생성 확인
                var orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
                assertThat(orders).hasSize(1);
                assertThat(orders.get(0).getFinalAmount()).isEqualByComparingTo(new BigDecimal("100000"));
                
                // 생성된 주문 ID 추가
                createdOrderIds.add(orders.get(0).getId());
        }

        @Test
        @DisplayName("재고가 부족한 상품으로 주문하려고 하면 실패한다")
        void 재고가_부족한_상품으로_주문하려고_하면_실패한다() {
                // Given
                Long userId = generateUniqueUserId();

                // 충분한 잔액 준비
                UserBalance userBalance = new UserBalance(userId);
                userBalance.charge(new BigDecimal("200000"));
                userBalanceRepository.save(userBalance);
                createdUserIds.add(userId);

                // 재고 1개만 있는 상품
                Product product = new Product(generateUniqueProductName("재고부족상품"), new BigDecimal("50000"), 1);
                Product savedProduct = productRepository.save(product);
                createdProductIds.add(savedProduct.getId());
                
                flushAndClear();

                // 5개 주문 시도 (재고 부족)
                CreateOrderRequest request = new CreateOrderRequest(
                                userId,
                                List.of(new OrderItemRequest(savedProduct.getId(), 5)),
                                null);

                // When
                ResponseEntity<CommonResponse> response = restTemplate.postForEntity(
                                "/api/v1/orders",
                                request,
                                CommonResponse.class);

                // Then: 400 에러 또는 다른 클라이언트 에러 확인
                assertThat(response.getStatusCode().is4xxClientError()).isTrue();
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isFalse();

                // 상태 변경 없음 확인
                var balance = userBalanceRepository.findByUserId(userId);
                assertThat(balance.get().getBalance()).isEqualByComparingTo(new BigDecimal("200000"));

                var productState = productRepository.findById(savedProduct.getId());
                assertThat(productState.get().getStockQuantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("잔액이 부족하면 주문을 완료할 수 없다")
        void 잔액이_부족하면_주문을_완료할_수_없다() {
                // Given
                Long userId = generateUniqueUserId();

                // 부족한 잔액 (30000원)
                UserBalance userBalance = new UserBalance(userId);
                userBalance.charge(new BigDecimal("30000"));
                userBalanceRepository.save(userBalance);
                createdUserIds.add(userId);

                // 50000원 상품
                Product product = new Product(generateUniqueProductName("고가상품"), new BigDecimal("50000"), 10);
                Product savedProduct = productRepository.save(product);
                createdProductIds.add(savedProduct.getId());
                
                flushAndClear();

                CreateOrderRequest request = new CreateOrderRequest(
                                userId,
                                List.of(new OrderItemRequest(savedProduct.getId(), 1)), // 50000원 주문
                                null);

                // When
                ResponseEntity<CommonResponse> response = restTemplate.postForEntity(
                                "/api/v1/orders",
                                request,
                                CommonResponse.class);

                // Then: 클라이언트 에러 확인
                assertThat(response.getStatusCode().is4xxClientError()).isTrue();
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isFalse();

                // 상태 변화 없음 확인
                var balance = userBalanceRepository.findByUserId(userId);
                assertThat(balance.get().getBalance()).isEqualByComparingTo(new BigDecimal("30000"));

                var productState = productRepository.findById(savedProduct.getId());
                assertThat(productState.get().getStockQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("사용자가 자신이 주문한 내역을 조회할 수 있다")
        void 사용자가_자신이_주문한_내역을_조회할_수_있다() {
                // Given: 사용자가 여러 주문을 생성
                Long userId = generateUniqueUserId();

                // 잔액 충전
                UserBalance userBalance = new UserBalance(userId);
                userBalance.charge(new BigDecimal("200000"));
                userBalanceRepository.save(userBalance);
                createdUserIds.add(userId);

                // 상품들 생성
                Product product1 = new Product(generateUniqueProductName("상품1"), new BigDecimal("30000"), 10);
                Product product2 = new Product(generateUniqueProductName("상품2"), new BigDecimal("40000"), 10);
                Product savedProduct1 = productRepository.save(product1);
                Product savedProduct2 = productRepository.save(product2);
                createdProductIds.add(savedProduct1.getId());
                createdProductIds.add(savedProduct2.getId());
                
                flushAndClear();

                // 첫 번째 주문
                CreateOrderRequest request1 = new CreateOrderRequest(
                                userId, List.of(new OrderItemRequest(savedProduct1.getId(), 1)), null);
                ResponseEntity<CommonResponse> orderResponse1 = restTemplate.postForEntity("/api/v1/orders", request1, CommonResponse.class);
                
                // 두 번째 주문  
                CreateOrderRequest request2 = new CreateOrderRequest(
                                userId, List.of(new OrderItemRequest(savedProduct2.getId(), 1)), null);
                ResponseEntity<CommonResponse> orderResponse2 = restTemplate.postForEntity("/api/v1/orders", request2, CommonResponse.class);
                
                // 주문이 성공적으로 생성되었는지 확인
                assertThat(orderResponse1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(orderResponse2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                
                // 생성된 주문들을 DB에서 찾아서 추적 리스트에 추가
                var createdOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
                for (var order : createdOrders) {
                        createdOrderIds.add(order.getId());
                }

                // When: 주문 목록 조회
                ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                                "/api/v1/orders/users/{userId}",
                                CommonResponse.class,
                                userId);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isTrue();

                // DB 검증 - 2개의 주문이 있어야 함
                var orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
                assertThat(orders).hasSize(2);
        }

        @Test
        @DisplayName("잘못된 요청으로 주문을 시도하면 실패한다")
        void 잘못된_요청으로_주문을_시도하면_실패한다() {
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
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isFalse();
        }
}