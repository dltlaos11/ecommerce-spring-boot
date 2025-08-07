package kr.hhplus.be.server.product.application;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.common.annotation.UseCase;
import kr.hhplus.be.server.product.dto.PopularProductResponse;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.product.service.ProductService;
import lombok.RequiredArgsConstructor;

/**
 * 상품 조회 UseCase - 단일 비즈니스 요구사항만 처리
 * 
 * 구체적인 요구사항들:
 * - "사용자가 상품 목록을 조회한다"
 * - "사용자가 상품을 검색한다"
 * - "사용자가 인기 상품을 조회한다"
 * - "사용자가 재고를 확인한다"
 */
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetProductsUseCase {

    private final ProductService productService;

    /**
     * 전체 상품 목록 조회
     */
    public List<ProductResponse> executeGetAll() {
        return productService.getAllProducts();
    }

    /**
     * 특정 상품 조회
     */
    public ProductResponse executeGet(Long productId) {
        return productService.getProduct(productId);
    }

    /**
     * 상품명 검색
     */
    public List<ProductResponse> executeSearch(String name) {
        return productService.searchProductsByName(name);
    }

    /**
     * 가격 범위 검색
     */
    public List<ProductResponse> executeSearchByPrice(BigDecimal minPrice, BigDecimal maxPrice) {
        return productService.getProductsByPriceRange(minPrice, maxPrice);
    }

    /**
     * 재고 있는 상품만 조회
     */
    public List<ProductResponse> executeGetAvailable() {
        return productService.getAvailableProducts();
    }

    /**
     * 인기 상품 조회
     */
    public List<PopularProductResponse> executeGetPopular(int limit, int days) {
        return productService.getPopularProducts(limit, days);
    }

    /**
     * 재고 확인
     */
    public boolean executeStockCheck(Long productId, int quantity) {
        return productService.hasEnoughStock(productId, quantity);
    }
}