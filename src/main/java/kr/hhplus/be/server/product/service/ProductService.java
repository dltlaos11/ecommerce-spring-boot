package kr.hhplus.be.server.product.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.repository.OrderItemRepository;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.dto.PopularProductResponse;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.product.exception.ProductNotFoundException;
import kr.hhplus.be.server.product.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 서비스 - 비관적 락 강화
 * 
 * 동시성 제어 전략:
 * - 재고 차감: SELECT FOR UPDATE (비관적 락)
 * - 조회: 일반 조회
 * - 복구: 일반 업데이트
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public ProductService(ProductRepository productRepository,
            OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
    }

    /**
     * 모든 상품 조회
     */
    public List<ProductResponse> getAllProducts() {
        log.debug("📋 전체 상품 목록 조회 요청");

        List<Product> products = productRepository.findAll();

        log.debug("✅ 총 {}개 상품 조회 완료", products.size());

        return products.stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * 특정 상품 조회
     */
    public ProductResponse getProduct(Long productId) {
        log.debug("🔍 상품 조회 요청: ID = {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("❌ 상품을 찾을 수 없음: ID = {}", productId);
                    return new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND);
                });

        log.debug("✅ 상품 조회 성공: {}", product.getName());

        return convertToResponse(product);
    }

    /**
     * 상품명으로 검색
     */
    public List<ProductResponse> searchProductsByName(String name) {
        log.debug("🔍 상품명 검색 요청: '{}'", name);

        if (name == null || name.trim().isEmpty()) {
            return getAllProducts();
        }

        List<Product> products = productRepository.findByNameContaining(name.trim());

        log.debug("✅ 상품명 검색 완료: '{}' - {}개 결과", name, products.size());

        return products.stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * 가격 범위로 상품 검색
     */
    public List<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("💰 가격 범위 검색 요청: {} ~ {}", minPrice, maxPrice);

        List<Product> products = productRepository.findByPriceBetween(minPrice, maxPrice);

        log.debug("✅ 가격 범위 검색 완료: {}개 결과", products.size());

        return products.stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * 재고가 있는 상품만 조회
     */
    public List<ProductResponse> getAvailableProducts() {
        log.debug("📦 재고 있는 상품 조회 요청");

        List<Product> products = productRepository.findByStockQuantityGreaterThan(0);

        log.debug("✅ 재고 있는 상품 조회 완료: {}개", products.size());

        return products.stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * 인기 상품 조회 (판매량 기준)
     */
    public List<PopularProductResponse> getPopularProducts(int limit, int days) {
        log.debug("📊 인기 상품 통계 생성: limit = {}, 기간 = {}일", limit, days);

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        // 1. 기간 내 모든 주문 항목 조회
        List<OrderItem> recentOrderItems = orderItemRepository.findAll().stream()
                .filter(item -> item.getCreatedAt().isAfter(startDate))
                .collect(Collectors.toList());

        log.debug("📈 기간 내 주문 항목 수: {}", recentOrderItems.size());

        // 2. 상품별 판매 통계 집계
        Map<Long, ProductSalesStats> salesStatsMap = recentOrderItems.stream()
                .collect(Collectors.groupingBy(
                        OrderItem::getProductId,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                this::calculateProductSalesStats)));

        // 3. 판매량 기준 내림차순 정렬 및 순위 부여
        AtomicInteger rank = new AtomicInteger(1);
        List<PopularProductResponse> popularProducts = salesStatsMap.values().stream()
                .sorted((a, b) -> Integer.compare(b.totalQuantity, a.totalQuantity))
                .limit(limit)
                .map(stats -> new PopularProductResponse(
                        rank.getAndIncrement(),
                        stats.productId,
                        stats.productName,
                        stats.productPrice,
                        stats.totalQuantity,
                        stats.totalAmount))
                .collect(Collectors.toList());

        log.info("✅ 인기 상품 통계 생성 완료: {}개", popularProducts.size());
        return popularProducts;
    }

    /**
     * 상품별 판매 통계 계산
     */
    private ProductSalesStats calculateProductSalesStats(List<OrderItem> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("주문 항목이 비어있습니다.");
        }

        OrderItem firstItem = items.get(0);
        Long productId = firstItem.getProductId();
        String productName = firstItem.getProductName();
        BigDecimal productPrice = firstItem.getProductPrice();

        int totalQuantity = items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        BigDecimal totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ProductSalesStats(productId, productName, productPrice, totalQuantity, totalAmount);
    }

    /**
     * 재고 확인
     */
    public boolean hasEnoughStock(Long productId, int quantity) {
        log.debug("📊 재고 확인 요청: 상품 ID = {}, 필요 수량 = {}", productId, quantity);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        boolean hasEnough = product.hasEnoughStock(quantity);

        log.debug("✅ 재고 확인 결과: {} (현재 재고: {})",
                hasEnough ? "충분" : "부족", product.getStockQuantity());

        return hasEnough;
    }

    /**
     * 재고 차감 - 비관적 락 강화
     * 🔒 SELECT FOR UPDATE로 동시 주문 시 정확한 재고 차감 보장
     */
    @Transactional
    public void reduceStock(Long productId, int quantity) {
        log.info("🔒 비관적 락 재고 차감 시작: 상품 ID = {}, 차감 수량 = {}", productId, quantity);

        // 비관적 락으로 상품 조회 (SELECT FOR UPDATE)
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> {
                    log.error("❌ 재고 차감 실패 - 상품 없음: ID = {}", productId);
                    return new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND);
                });

        int beforeStock = product.getStockQuantity();

        // 도메인 객체의 비즈니스 로직 호출
        product.reduceStock(quantity);

        // 변경된 상품 저장
        productRepository.save(product);

        log.info("✅ 비관적 락 재고 차감 완료: 상품 '{}', {} → {} (차감: {})",
                product.getName(), beforeStock, product.getStockQuantity(), quantity);
    }

    /**
     * 재고 차감 검증 (재고 부족 시 예외 발생)
     */
    @Transactional
    public void reduceStockWithValidation(Long productId, int quantity) {
        log.info("🔒 검증 포함 재고 차감: 상품 ID = {}, 수량 = {}", productId, quantity);

        // 비관적 락으로 상품 조회
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> {
                    log.error("❌ 상품을 찾을 수 없음: ID = {}", productId);
                    return new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND);
                });

        // 재고 부족 검증
        if (!product.hasEnoughStock(quantity)) {
            log.warn("❌ 재고 부족: 상품 '{}', 요청 {}, 현재 {}",
                    product.getName(), quantity, product.getStockQuantity());
            throw new kr.hhplus.be.server.product.exception.InsufficientStockException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    String.format("재고 부족: 상품 '%s', 요청 %d, 현재 %d",
                            product.getName(), quantity, product.getStockQuantity()));
        }

        int beforeStock = product.getStockQuantity();
        product.reduceStock(quantity);
        productRepository.save(product);

        log.info("✅ 검증 포함 재고 차감 완료: '{}', {} → {}",
                product.getName(), beforeStock, product.getStockQuantity());
    }

    /**
     * 다중 상품 재고 차감 - 데드락 방지를 위한 ID 정렬
     */
    @Transactional
    public void reduceMultipleStocks(Map<Long, Integer> productQuantityMap) {
        log.info("🔒 다중 상품 재고 차감 시작: {} 개 상품", productQuantityMap.size());

        // 데드락 방지를 위해 상품 ID 순으로 정렬하여 처리
        List<Long> sortedProductIds = productQuantityMap.keySet().stream()
                .sorted()
                .toList();

        for (Long productId : sortedProductIds) {
            int quantity = productQuantityMap.get(productId);
            reduceStockWithValidation(productId, quantity);
        }

        log.info("✅ 다중 상품 재고 차감 완료");
    }

    /**
     * 재고 복구 (주문 취소, 결제 실패 시)
     */
    @Transactional
    public void restoreStock(Long productId, int quantity) {
        log.debug("📈 재고 복구 요청: 상품 ID = {}, 복구 수량 = {}", productId, quantity);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("❌ 재고 복구 실패 - 상품 없음: ID = {}", productId);
                    return new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND);
                });

        int beforeStock = product.getStockQuantity();

        product.restoreStock(quantity);
        productRepository.save(product);

        log.info("✅ 재고 복구 완료: 상품 '{}', {} → {} (복구: {})",
                product.getName(), beforeStock, product.getStockQuantity(), quantity);
    }

    /**
     * 상품 생성 (관리자 기능)
     */
    @Transactional
    public ProductResponse createProduct(String name, BigDecimal price, Integer stockQuantity) {
        log.debug("🆕 상품 생성 요청: '{}', 가격 {}, 재고 {}", name, price, stockQuantity);

        Product product = new Product(name, price, stockQuantity);
        Product savedProduct = productRepository.save(product);

        log.info("✅ 상품 생성 완료: ID = {}, 이름 = '{}'", savedProduct.getId(), savedProduct.getName());

        return convertToResponse(savedProduct);
    }

    /**
     * 상품 정보 수정 (관리자 기능)
     */
    @Transactional
    public ProductResponse updateProduct(Long productId, String name, BigDecimal price) {
        log.debug("✏️ 상품 수정 요청: ID = {}, 이름 = '{}', 가격 = {}", productId, name, price);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        String oldName = product.getName();
        BigDecimal oldPrice = product.getPrice();

        product.updateProductInfo(name, price);
        Product savedProduct = productRepository.save(product);

        log.info("✅ 상품 수정 완료: '{}' → '{}', {} → {}",
                oldName, name, oldPrice, price);

        return convertToResponse(savedProduct);
    }

    /**
     * 상품 삭제 (관리자 기능)
     */
    @Transactional
    public void deleteProduct(Long productId) {
        log.debug("🗑️ 상품 삭제 요청: ID = {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        String productName = product.getName();
        productRepository.delete(product);

        log.info("✅ 상품 삭제 완료: ID = {}, 이름 = '{}'", productId, productName);
    }

    /**
     * Domain 객체를 DTO로 변환
     */
    private ProductResponse convertToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getCreatedAt());
    }

    /**
     * 상품별 판매 통계 내부 클래스
     */
    private static class ProductSalesStats {
        final Long productId;
        final String productName;
        final BigDecimal productPrice;
        final Integer totalQuantity;
        final BigDecimal totalAmount;

        ProductSalesStats(Long productId, String productName, BigDecimal productPrice,
                Integer totalQuantity, BigDecimal totalAmount) {
            this.productId = productId;
            this.productName = productName;
            this.productPrice = productPrice;
            this.totalQuantity = totalQuantity;
            this.totalAmount = totalAmount;
        }
    }
}