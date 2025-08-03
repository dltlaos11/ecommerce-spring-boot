package kr.hhplus.be.server.product.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.product.application.ProductUseCase;
import kr.hhplus.be.server.product.dto.PopularProductResponse;
import kr.hhplus.be.server.product.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Application Layer 적용
 * 변경사항:
 * - ProductService → ProductUseCase 의존성 변경
 * - HTTP 요청/응답 처리에만 집중
 * - 재고 확인 API 추가
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "상품 관리", description = "상품 조회 및 검색 API")
@RequiredArgsConstructor
public class ProductController {

  private final ProductUseCase productUseCase;

  /**
   * 상품 목록 조회 (필터링 지원)
   */
  @GetMapping
  @Operation(summary = "상품 목록 조회", description = "상품 목록을 조회합니다. 다양한 필터링 옵션을 제공합니다.")
  public CommonResponse<List<ProductResponse>> getProducts(
      @Parameter(description = "상품명 검색 (부분 일치)", example = "노트북") @RequestParam(required = false) String name,
      @Parameter(description = "최소 가격", example = "100000") @RequestParam(required = false) BigDecimal minPrice,
      @Parameter(description = "최대 가격", example = "2000000") @RequestParam(required = false) BigDecimal maxPrice,
      @Parameter(description = "재고 있는 상품만 조회", example = "true") @RequestParam(required = false, defaultValue = "false") Boolean onlyAvailable) {

    log.info("📋 상품 목록 조회 요청 - name: '{}', 가격범위: {} ~ {}, 재고필터: {}",
        name, minPrice, maxPrice, onlyAvailable);

    List<ProductResponse> products;

    if (name != null && !name.trim().isEmpty()) {
      products = productUseCase.searchProductsByName(name);
    } else if (minPrice != null && maxPrice != null) {
      products = productUseCase.getProductsByPriceRange(minPrice, maxPrice);
    } else if (onlyAvailable) {
      products = productUseCase.getAvailableProducts();
    } else {
      products = productUseCase.getAllProducts();
    }

    log.info("✅ 상품 목록 조회 완료 - {}개 상품", products.size());

    return CommonResponse.success(products);
  }

  /**
   * 특정 상품 상세 조회
   */
  @GetMapping("/{productId}")
  @Operation(summary = "상품 상세 조회", description = "특정 상품의 상세 정보를 조회합니다.")
  public CommonResponse<ProductResponse> getProduct(
      @Parameter(description = "상품 ID", example = "1", required = true) @PathVariable Long productId) {

    log.info("🔍 상품 상세 조회 요청 - ID: {}", productId);

    ProductResponse product = productUseCase.getProduct(productId);

    log.info("✅ 상품 상세 조회 완료 - ID: {}, 이름: '{}'", productId, product.name());

    return CommonResponse.success(product);
  }

  /**
   * 재고 확인 API (주문 전 재고 체크용) - 🆕 새로 추가
   */
  @GetMapping("/{productId}/stock")
  @Operation(summary = "상품 재고 확인", description = "특정 상품의 재고가 충분한지 확인합니다.")
  public CommonResponse<StockCheckResponse> checkStock(
      @Parameter(description = "상품 ID", example = "1", required = true) @PathVariable Long productId,
      @Parameter(description = "확인할 수량", example = "3", required = true) @RequestParam int quantity) {

    log.info("📊 재고 확인 요청 - 상품 ID: {}, 필요 수량: {}", productId, quantity);

    ProductResponse product = productUseCase.getProduct(productId);
    boolean available = productUseCase.hasEnoughStock(productId, quantity);

    StockCheckResponse response = new StockCheckResponse(
        productId, quantity, product.stockQuantity(), available);

    log.info("✅ 재고 확인 완료 - 상품 ID: {}, 결과: {}", productId, available ? "충분" : "부족");

    return CommonResponse.success(response);
  }

  /**
   * 인기 상품 조회
   */
  @GetMapping("/popular")
  @Operation(summary = "인기 상품 조회", description = "판매량 기준 인기 상품 상위 N개를 조회합니다.")
  public CommonResponse<List<PopularProductResponse>> getPopularProducts(
      @Parameter(description = "조회할 상품 개수", example = "5") @RequestParam(defaultValue = "5") int limit,
      @Parameter(description = "조회 기간 (일)", example = "7") @RequestParam(defaultValue = "30") int days) {

    // 파라미터 검증
    if (limit <= 0 || limit > 100) {
      throw new IllegalArgumentException("조회 개수는 1-100 사이여야 합니다.");
    }
    if (days <= 0 || days > 365) {
      throw new IllegalArgumentException("조회 기간은 1-365일 사이여야 합니다.");
    }

    log.info("📊 인기 상품 조회 요청 - limit: {}, 기간: {}일", limit, days);

    List<PopularProductResponse> popularProducts = productUseCase.getPopularProducts(limit, days);

    log.info("✅ 인기 상품 조회 완료 - {}개 상품", popularProducts.size());

    return CommonResponse.success(popularProducts);
  }

  /**
   * 재고 확인 응답 DTO - 🆕 추가
   */
  @Schema(description = "재고 확인 응답")
  public static record StockCheckResponse(
      @Schema(description = "상품 ID", example = "1") Long productId,
      @Schema(description = "요청한 수량", example = "3") Integer requestedQuantity,
      @Schema(description = "현재 재고", example = "10") Integer currentStock,
      @Schema(description = "재고 충분 여부", example = "true") Boolean available) {
  }
}