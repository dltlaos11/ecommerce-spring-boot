package kr.hhplus.be.server.product.application;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.common.annotation.UseCase;
import kr.hhplus.be.server.product.dto.PopularProductResponse;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Application Layer - 상품 비즈니스 Usecase
 * 
 * 책임:
 * - 상품 관련 비즈니스 유스케이스 구현
 * - 재고 관리 유스케이스
 * - 인기 상품 통계 처리
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductUseCase {

    private final ProductService productService;

    /**
     * 모든 상품 조회 유스케이스
     */
    public List<ProductResponse> getAllProducts() {
        log.debug("📋 전체 상품 조회 유스케이스");
        return productService.getAllProducts();
    }

    /**
     * 특정 상품 조회 유스케이스
     */
    public ProductResponse getProduct(Long productId) {
        log.debug("🔍 상품 조회 유스케이스: productId = {}", productId);
        return productService.getProduct(productId);
    }

    /**
     * 상품명 검색 유스케이스
     */
    public List<ProductResponse> searchProductsByName(String name) {
        log.debug("🔍 상품명 검색 유스케이스: name = '{}'", name);
        return productService.searchProductsByName(name);
    }

    /**
     * 가격 범위 검색 유스케이스
     */
    public List<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("💰 가격 범위 검색 유스케이스: {} ~ {}", minPrice, maxPrice);
        return productService.getProductsByPriceRange(minPrice, maxPrice);
    }

    /**
     * 재고 있는 상품 조회 유스케이스
     */
    public List<ProductResponse> getAvailableProducts() {
        log.debug("📦 재고 있는 상품 조회 유스케이스");
        return productService.getAvailableProducts();
    }

    /**
     * 인기 상품 조회 유스케이스
     */
    public List<PopularProductResponse> getPopularProducts(int limit, int days) {
        log.debug("📊 인기 상품 조회 유스케이스: limit = {}, days = {}", limit, days);
        return productService.getPopularProducts(limit, days);
    }

    /**
     * 재고 확인 유스케이스
     */
    public boolean hasEnoughStock(Long productId, int quantity) {
        log.debug("📊 재고 확인 유스케이스: productId = {}, quantity = {}", productId, quantity);
        return productService.hasEnoughStock(productId, quantity);
    }
}