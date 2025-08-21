package kr.hhplus.be.server.support;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import kr.hhplus.be.server.balance.dto.BalanceResponse;
import kr.hhplus.be.server.balance.dto.ChargeBalanceRequest;
import kr.hhplus.be.server.balance.dto.ChargeBalanceResponse;
import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.order.dto.CreateOrderRequest;
import kr.hhplus.be.server.order.dto.OrderItemRequest;
import kr.hhplus.be.server.order.dto.OrderResponse;
import kr.hhplus.be.server.product.dto.ProductResponse;

/**
 * 비즈니스 중심 테스트 헬퍼
 * 
 * 목적:
 * - API 기반으로 비즈니스 행동 테스트
 * - 구현 세부사항(Repository, 캐시 등)에서 분리
 * - "What"에 집중, "How"는 숨김
 */
@Component
public class BusinessTestHelper {

    private final TestRestTemplate restTemplate;

    public BusinessTestHelper(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ==================== 사용자 잔액 비즈니스 ====================

    /**
     * 사용자 잔액 충전 (비즈니스 행동)
     */
    public ChargeBalanceResponse 잔액_충전(Long userId, BigDecimal amount) {
        ChargeBalanceRequest request = new ChargeBalanceRequest(amount);

        ResponseEntity<CommonResponse<ChargeBalanceResponse>> response = restTemplate.exchange(
                "/api/v1/users/{userId}/balance/charge",
                HttpMethod.POST,
                createHttpEntity(request),
                new ParameterizedTypeReference<CommonResponse<ChargeBalanceResponse>>() {
                },
                userId);

        assertSuccessResponse(response, "잔액 충전");
        return response.getBody().getData();
    }

    /**
     * 사용자 잔액 조회 (비즈니스 행동)
     */
    public BalanceResponse 잔액_조회(Long userId) {
        ResponseEntity<CommonResponse<BalanceResponse>> response = restTemplate.exchange(
                "/api/v1/users/{userId}/balance",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<CommonResponse<BalanceResponse>>() {
                },
                userId);

        assertSuccessResponse(response, "잔액 조회");
        return response.getBody().getData();
    }

    // ==================== 상품 비즈니스 ====================

    /**
     * 상품 목록 조회 (비즈니스 행동)
     */
    public List<ProductResponse> 상품_목록_조회() {
        ResponseEntity<CommonResponse<List<ProductResponse>>> response = restTemplate.exchange(
                "/api/v1/products",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<CommonResponse<List<ProductResponse>>>() {
                });

        assertSuccessResponse(response, "상품 목록 조회");
        return response.getBody().getData();
    }

    /**
     * 특정 상품 조회 (비즈니스 행동)
     */
    public ProductResponse 상품_조회(Long productId) {
        ResponseEntity<CommonResponse<ProductResponse>> response = restTemplate.exchange(
                "/api/v1/products/{productId}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<CommonResponse<ProductResponse>>() {
                },
                productId);

        assertSuccessResponse(response, "상품 조회");
        return response.getBody().getData();
    }

    /**
     * 존재하지 않는 상품 조회 (실패 케이스 검증)
     */
    public ResponseEntity<CommonResponse<ProductResponse>> 존재하지_않는_상품_조회_시도(Long productId) {
        return restTemplate.exchange(
                "/api/v1/products/{productId}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<CommonResponse<ProductResponse>>() {
                },
                productId);
    }

    // ==================== 주문 비즈니스 ====================

    /**
     * 주문 생성 (비즈니스 행동)
     */
    public OrderResponse 주문_생성(Long userId, Long productId, int quantity) {
        List<OrderItemRequest> orderItems = List.of(
                new OrderItemRequest(productId, quantity));
        CreateOrderRequest request = new CreateOrderRequest(userId, orderItems, null);

        ResponseEntity<CommonResponse<OrderResponse>> response = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                createHttpEntity(request),
                new ParameterizedTypeReference<CommonResponse<OrderResponse>>() {
                });

        assertSuccessResponse(response, "주문 생성");
        return response.getBody().getData();
    }

    /**
     * 주문 생성 시도 (실패 케이스 포함)
     */
    public ResponseEntity<CommonResponse<OrderResponse>> 주문_생성_시도(Long userId, Long productId, int quantity) {
        List<OrderItemRequest> orderItems = List.of(
                new OrderItemRequest(productId, quantity));
        CreateOrderRequest request = new CreateOrderRequest(userId, orderItems, null);

        return restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                createHttpEntity(request),
                new ParameterizedTypeReference<CommonResponse<OrderResponse>>() {
                });
    }

    /**
     * 사용자 주문 내역 조회 (비즈니스 행동)
     */
    public List<OrderResponse> 주문_내역_조회(Long userId) {
        ResponseEntity<CommonResponse<List<OrderResponse>>> response = restTemplate.exchange(
                "/api/v1/orders/users/{userId}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<CommonResponse<List<OrderResponse>>>() {
                },
                userId);

        assertSuccessResponse(response, "주문 내역 조회");
        return response.getBody().getData();
    }

    // ==================== 헬퍼 메서드 ====================

    /**
     * 테스트용 고유 사용자 ID 생성
     */
    public Long 테스트_사용자_생성() {
        return ThreadLocalRandom.current().nextLong(100000, 999999);
    }

    /**
     * 테스트용 상품 ID 조회 (기존 상품 중 사용)
     */
    public Long 테스트_상품_선택() {
        List<ProductResponse> products = 상품_목록_조회();
        if (products.isEmpty()) {
            throw new IllegalStateException("테스트용 상품이 없습니다. 데이터 초기화가 필요합니다.");
        }
        return products.get(0).id();
    }

    /**
     * 충분한 재고가 있는 상품 선택
     */
    public ProductResponse 재고_충분한_상품_선택(int 필요한_수량) {
        List<ProductResponse> products = 상품_목록_조회();
        return products.stream()
                .filter(product -> product.stockQuantity() >= 필요한_수량)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("충분한 재고가 있는 상품이 없습니다."));
    }

    /**
     * 가격 범위 내에서 충분한 재고가 있는 상품 선택
     */
    public ProductResponse 가격_범위내_재고_충분한_상품_선택(int 필요한_수량, BigDecimal 최대가격) {
        List<ProductResponse> products = 상품_목록_조회();
        return products.stream()
                .filter(product -> product.stockQuantity() >= 필요한_수량)
                .filter(product -> product.price().compareTo(최대가격) <= 0)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        String.format("가격 %s원 이하이면서 재고 %d개 이상인 상품이 없습니다.", 최대가격, 필요한_수량)));
    }

    /**
     * 재고 부족한 상품 선택
     */
    public ProductResponse 재고_부족한_상품_선택(int 요청_수량) {
        List<ProductResponse> products = 상품_목록_조회();
        return products.stream()
                .filter(product -> product.stockQuantity() < 요청_수량)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("재고 부족한 상품이 없습니다."));
    }

    // ==================== 내부 헬퍼 ====================

    private org.springframework.http.HttpEntity<?> createHttpEntity(Object body) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new org.springframework.http.HttpEntity<>(body, headers);
    }

    private void assertSuccessResponse(ResponseEntity<?> response, String operation) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError(String.format("%s 실패: 상태코드=%s, 응답=%s",
                    operation, response.getStatusCode(), response.getBody()));
        }
    }
}