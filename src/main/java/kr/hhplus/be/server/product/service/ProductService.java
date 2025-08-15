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
import kr.hhplus.be.server.product.cache.ProductCacheService;
import kr.hhplus.be.server.product.dto.PopularProductResponse;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.product.exception.ProductNotFoundException;
import kr.hhplus.be.server.product.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;

// 분산락 + 캐시 기반 상품 관리
@Slf4j
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductCacheService productCacheService;

    public ProductService(ProductRepository productRepository,
            OrderItemRepository orderItemRepository,
            ProductCacheService productCacheService) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.productCacheService = productCacheService;
    }

    public List<ProductResponse> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(this::convertToResponse)
                .toList();
    }

    public ProductResponse getProduct(Long productId) {
        Product product = productCacheService.findProductById(productId)
                .orElseThrow(() -> new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));
        return convertToResponse(product);
    }

    public List<ProductResponse> searchProductsByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getAllProducts();
        }

        List<Product> products = productRepository.findByNameContaining(name.trim());
        return products.stream()
                .map(this::convertToResponse)
                .toList();
    }

    public List<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        List<Product> products = productRepository.findByPriceBetween(minPrice, maxPrice);
        return products.stream()
                .map(this::convertToResponse)
                .toList();
    }

    public List<ProductResponse> getAvailableProducts() {
        List<Product> products = productRepository.findByStockQuantityGreaterThan(0);
        return products.stream()
                .map(this::convertToResponse)
                .toList();
    }

    public List<PopularProductResponse> getPopularProducts(int limit, int days) {

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        // 1. 기간 내 모든 주문 항목 조회
        List<OrderItem> recentOrderItems = orderItemRepository.findAll().stream()
                .filter(item -> item.getCreatedAt().isAfter(startDate))
                .collect(Collectors.toList());


        // 2. 상품별 판매 통계 집계
        Map<Long, ProductSalesStats> salesStatsMap = recentOrderItems.stream()
                .collect(Collectors.groupingBy(
                        OrderItem::getProductId,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                this::calculateProductSalesStats)));

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

        return popularProducts;
    }

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

    public boolean hasEnoughStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        return product.hasEnoughStock(quantity);
    }

    // 분산락 기반 재고 차감 (비관적 락 제거)
    public void reduceStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        product.reduceStock(quantity);
        productRepository.save(product);
        
        // 재고 변경 시 캐시 무효화
        productCacheService.evictProductCache(productId);
    }

    public void reduceStockWithValidation(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.hasEnoughStock(quantity)) {
            log.warn("재고 부족: 상품 '{}', 요청 {}, 현재 {}",
                    product.getName(), quantity, product.getStockQuantity());
            throw new kr.hhplus.be.server.product.exception.InsufficientStockException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    String.format("재고 부족: 상품 '%s', 요청 %d, 현재 %d",
                            product.getName(), quantity, product.getStockQuantity()));
        }

        product.reduceStock(quantity);
        productRepository.save(product);
        
        // 재고 변경 시 캐시 무효화
        productCacheService.evictProductCache(productId);
    }

    @Transactional
    public void restoreStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        product.restoreStock(quantity);
        productRepository.save(product);
        
        // 재고 복원 시 캐시 무효화
        productCacheService.evictProductCache(productId);
    }

    @Transactional
    public ProductResponse createProduct(String name, BigDecimal price, Integer stockQuantity) {
        Product product = new Product(name, price, stockQuantity);
        Product savedProduct = productRepository.save(product);

        return convertToResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, String name, BigDecimal price) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        product.updateProductInfo(name, price);
        Product savedProduct = productRepository.save(product);

        return convertToResponse(savedProduct);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        productRepository.delete(product);
    }

    private ProductResponse convertToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getCreatedAt());
    }

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