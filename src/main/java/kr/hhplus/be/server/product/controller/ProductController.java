// src/main/java/kr/hhplus/be/server/product/controller/ProductController.java
package kr.hhplus.be.server.product.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.product.dto.PopularProductResponse;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 컨트롤러 - 실제 Service 연동 버전
 * 
 * ✨ 변경사항:
 * - Mock 데이터 제거
 * - ProductService 의존성 주입 및 실제 호출
 * - HTTP 요청/응답 처리에만 집중
 * 
 * 🎯 책임:
 * - HTTP 요청 파라미터 검증
 * - Service 호출 및 결과 반환
 * - 예외 처리는 @ControllerAdvice에 위임
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "상품 관리", description = "상품 조회 및 검색 API")
public class ProductController {

  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  /**
   * 상품 목록 조회 (필터링 지원)
   */
  @GetMapping
  @Operation(summary = "상품 목록 조회", description = "상품 목록을 조회합니다. 다양한 필터링 옵션을 제공합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "상품 목록 조회 성공", value = """
          {
            "success": true,
            "data": [
              {
                "id": 1,
                "name": "고성능 노트북",
                "price": 1500000.00,
                "stockQuantity": 10,
                "createdAt": "2025-01-20T10:30:00"
              }
            ],
            "timestamp": "2025-01-20T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "잘못된 파라미터", value = """
          {
            "success": false,
            "error": {
              "code": "INVALID_PARAMETER",
              "message": "잘못된 요청 파라미터입니다."
            },
            "timestamp": "2025-01-20T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "서버 오류", value = """
          {
            "success": false,
            "error": {
              "code": "INTERNAL_ERROR",
              "message": "서버 내부 오류가 발생했습니다."
            },
            "timestamp": "2025-01-20T10:30:00"
          }
          """)))
  })
  public CommonResponse<List<ProductResponse>> getProducts(
      @Parameter(description = "상품명 검색 (부분 일치)", example = "노트북") @RequestParam(required = false) String name,

      @Parameter(description = "최소 가격", example = "100000") @RequestParam(required = false) BigDecimal minPrice,

      @Parameter(description = "최대 가격", example = "2000000") @RequestParam(required = false) BigDecimal maxPrice,

      @Parameter(description = "재고 있는 상품만 조회", example = "true") @RequestParam(required = false, defaultValue = "false") Boolean onlyAvailable) {

    log.info("📋 상품 목록 조회 요청 - name: '{}', 가격범위: {} ~ {}, 재고필터: {}",
        name, minPrice, maxPrice, onlyAvailable);

    List<ProductResponse> products;

    if (name != null && !name.trim().isEmpty()) {
      products = productService.searchProductsByName(name);
    } else if (minPrice != null && maxPrice != null) {
      products = productService.getProductsByPriceRange(minPrice, maxPrice);
    } else if (onlyAvailable) {
      products = productService.getAvailableProducts();
    } else {
      products = productService.getAllProducts();
    }

    log.info("✅ 상품 목록 조회 완료 - {}개 상품", products.size());

    return CommonResponse.success(products);
  }

  /**
   * 특정 상품 상세 조회
   */
  @GetMapping("/{productId}")
  @Operation(summary = "상품 상세 조회", description = "특정 상품의 상세 정보를 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "상품 상세 조회 성공", value = """
          {
            "success": true,
            "data": {
              "id": 1,
              "name": "고성능 노트북",
              "price": 1500000.00,
              "stockQuantity": 10,
              "createdAt": "2025-01-20T10:30:00"
            },
            "timestamp": "2025-01-20T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "400", description = "잘못된 상품 ID", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "잘못된 ID", value = """
          {
            "success": false,
            "error": {
              "code": "INVALID_PARAMETER",
              "message": "상품 ID가 올바르지 않습니다."
            },
            "timestamp": "2025-01-20T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "상품 없음", value = """
          {
            "success": false,
            "error": {
              "code": "PRODUCT_NOT_FOUND",
              "message": "상품을 찾을 수 없습니다."
            },
            "timestamp": "2025-01-20T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "서버 오류", value = """
          {
            "success": false,
            "error": {
              "code": "INTERNAL_ERROR",
              "message": "서버 내부 오류가 발생했습니다."
            },
            "timestamp": "2025-01-20T10:30:00"
          }
          """)))
  })
  public CommonResponse<ProductResponse> getProduct(
      @Parameter(description = "상품 ID", example = "1", required = true) @PathVariable Long productId) {

    log.info("🔍 상품 상세 조회 요청 - ID: {}", productId);

    ProductResponse product = productService.getProduct(productId);

    log.info("✅ 상품 상세 조회 완료 - ID: {}, 이름: '{}'", productId, product.name());

    return CommonResponse.success(product);
  }

  /**
   * 재고 확인 API (주문 전 재고 체크용)
   */
  @GetMapping("/{productId}/stock")
  @Operation(summary = "상품 재고 확인", description = "특정 상품의 재고가 충분한지 확인합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "재고 확인 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "재고 확인 성공", value = """
          {
            "success": true,
            "data": {
              "productId": 1,
              "requestedQuantity": 3,
              "currentStock": 10,
              "available": true
            },
            "timestamp": "2025-01-20T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "잘못된 상품 ID", value = """
              {
                "success": false,
                "error": {
                  "code": "INVALID_PARAMETER",
                  "message": "상품 ID가 올바르지 않습니다."
                },
                "timestamp": "2025-01-20T10:30:00"
              }
              """),
          @ExampleObject(name = "잘못된 수량", value = """
              {
                "success": false,
                "error": {
                  "code": "INVALID_PARAMETER",
                  "message": "수량은 1 이상이어야 합니다."
                },
                "timestamp": "2025-01-20T10:30:00"
              }
              """)
      })),
      @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "상품 없음", value = """
          {
            "success": false,
            "error": {
              "code": "PRODUCT_NOT_FOUND",
              "message": "상품을 찾을 수 없습니다."
            },
            "timestamp": "2025-01-20T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "서버 오류", value = """
          {
            "success": false,
            "error": {
              "code": "INTERNAL_ERROR",
              "message": "서버 내부 오류가 발생했습니다."
            },
            "timestamp": "2025-01-20T10:30:00"
          }
          """)))
  })
  public CommonResponse<StockCheckResponse> checkStock(
      @Parameter(description = "상품 ID", example = "1", required = true) @PathVariable Long productId,

      @Parameter(description = "확인할 수량", example = "3", required = true) @RequestParam int quantity) {

    log.info("📊 재고 확인 요청 - 상품 ID: {}, 필요 수량: {}", productId, quantity);

    ProductResponse product = productService.getProduct(productId);
    boolean available = productService.hasEnoughStock(productId, quantity);

    StockCheckResponse response = new StockCheckResponse(
        productId,
        quantity,
        product.stockQuantity(),
        available);

    log.info("✅ 재고 확인 완료 - 상품 ID: {}, 결과: {}", productId, available ? "충분" : "부족");

    return CommonResponse.success(response);
  }

  @GetMapping("/popular")
  @Operation(summary = "인기 상품 조회", description = "판매량 기준 인기 상품 상위 N개를 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "인기 상품 목록", value = """
          {
            "success": true,
            "data": [
              {
                "rank": 1,
                "productId": 1,
                "productName": "고성능 노트북",
                "price": 1500000.00,
                "totalSalesQuantity": 150,
                "totalSalesAmount": 225000000.00
              }
            ],
            "timestamp": "2025-07-29T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class),

          examples = @ExampleObject(name = "잘못된 파라미터", value = """
              {
                "success": false,
                "error": {
                  "code": "INVALID_PARAMETER",
                  "message": "잘못된 요청 파라미터입니다."
                },
                "timestamp": "2025-07-29T10:30:00"
              }
              """))),
  })
  public CommonResponse<List<PopularProductResponse>> getPopularProducts(
      @Parameter(description = "조회할 상품 개수", example = "5") @RequestParam(defaultValue = "5") int limit,

      @Parameter(description = "조회 기간 (일)", example = "7") @RequestParam(defaultValue = "30") int days) {

    // 파라미터 검증 (실제 400 에러 발생용)
    if (limit <= 0 || limit > 100) {
      throw new IllegalArgumentException("조회 개수는 1-100 사이여야 합니다.");
    }

    if (days <= 0 || days > 365) {
      throw new IllegalArgumentException("조회 기간은 1-365일 사이여야 합니다.");
    }

    log.info("📊 인기 상품 조회 요청 - limit: {}, 기간: {}일", limit, days);

    List<PopularProductResponse> popularProducts = productService.getPopularProducts(limit, days);

    log.info("✅ 인기 상품 조회 완료 - {}개 상품", popularProducts.size());

    return CommonResponse.success(popularProducts);
  }

  /**
   * 재고 확인 응답 DTO
   */
  public record StockCheckResponse(
      Long productId,
      Integer requestedQuantity,
      Integer currentStock,
      Boolean available) {
  }
}