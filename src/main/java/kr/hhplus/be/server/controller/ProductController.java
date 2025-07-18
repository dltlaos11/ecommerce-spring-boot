package kr.hhplus.be.server.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.dto.product.ProductResponse;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "상품 관리", description = "상품 조회 API")
public class ProductController {

  @GetMapping
  @Operation(summary = "상품 목록 조회", description = "상품 목록을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = """
          {
            "success": true,
            "data": [
              {
                "id": 1,
                "name": "고성능 노트북",
                "price": 1500000.00,
                "stockQuantity": 10,
                "createdAt": "2025-07-07T10:30:00"
              },
              {
                "id": 2,
                "name": "무선 마우스",
                "price": 50000.00,
                "stockQuantity": 50,
                "createdAt": "2025-07-12T10:30:00"
              },
              {
                "id": 3,
                "name": "기계식 키보드",
                "price": 150000.00,
                "stockQuantity": 25,
                "createdAt": "2025-07-14T10:30:00"
              }
            ],
            "error": null,
            "timestamp": "2025-07-17T10:30:00"
          }
          """)))
  })
  public CommonResponse<List<ProductResponse>> getProducts(
      @Parameter(description = "상품명 검색", example = "노트북") @RequestParam(required = false) String name,
      @Parameter(description = "최소 가격", example = "100000") @RequestParam(required = false) BigDecimal minPrice,
      @Parameter(description = "최대 가격", example = "2000000") @RequestParam(required = false) BigDecimal maxPrice) {

    // 전체 Mock 데이터 생성
    List<ProductResponse> allProducts = List.of(
        new ProductResponse(1L, "고성능 노트북", new BigDecimal("1500000.00"), 10, LocalDateTime.now().minusDays(10)),
        new ProductResponse(2L, "무선 마우스", new BigDecimal("50000.00"), 50, LocalDateTime.now().minusDays(5)),
        new ProductResponse(3L, "기계식 키보드", new BigDecimal("150000.00"), 25, LocalDateTime.now().minusDays(3)),
        new ProductResponse(4L, "27인치 모니터", new BigDecimal("300000.00"), 15, LocalDateTime.now().minusDays(7)),
        new ProductResponse(5L, "HD 웹캠", new BigDecimal("80000.00"), 30, LocalDateTime.now().minusDays(2)),
        new ProductResponse(6L, "게이밍 노트북", new BigDecimal("2500000.00"), 5, LocalDateTime.now().minusDays(1)));

    // 파라미터별 필터링 적용
    List<ProductResponse> filteredProducts = allProducts.stream()
        .filter(product -> {
          // 상품명 필터링
          if (name != null && !name.trim().isEmpty()) {
            return product.getName().toLowerCase().contains(name.toLowerCase());
          }
          return true;
        })
        .filter(product -> {
          // 최소 가격 필터링
          if (minPrice != null) {
            return product.getPrice().compareTo(minPrice) >= 0;
          }
          return true;
        })
        .filter(product -> {
          // 최대 가격 필터링
          if (maxPrice != null) {
            return product.getPrice().compareTo(maxPrice) <= 0;
          }
          return true;
        })
        .toList();

    return CommonResponse.success(filteredProducts);
  }

  @GetMapping("/{productId}")
  @Operation(summary = "상품 상세 조회", description = "특정 상품의 상세 정보를 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = """
          {
            "success": true,
            "data": {
              "id": 1,
              "name": "고성능 노트북",
              "price": 1500000.00,
              "stockQuantity": 10,
              "createdAt": "2025-07-07T10:30:00"
            },
            "error": null,
            "timestamp": "2025-07-17T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = """
          {
            "success": false,
            "data": null,
            "error": {
              "code": "PRODUCT_NOT_FOUND",
              "message": "상품을 찾을 수 없습니다."
            },
            "timestamp": "2025-07-17T10:30:00"
          }
          """)))
  })
  public CommonResponse<ProductResponse> getProduct(
      @Parameter(description = "상품 ID", example = "1") @PathVariable Long productId) {

    // Mock 데이터 생성
    ProductResponse product = new ProductResponse(
        productId,
        "고성능 노트북",
        new BigDecimal("1500000.00"),
        10,
        LocalDateTime.now().minusDays(10));

    return CommonResponse.success(product);
  }
}