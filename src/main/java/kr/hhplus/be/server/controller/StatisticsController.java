package kr.hhplus.be.server.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.dto.statistics.PopularProductResponse;

@RestController
@RequestMapping("/api/v1/statistics")
@Tag(name = "통계", description = "판매 통계 및 인기 상품 조회 API")
public class StatisticsController {

  @GetMapping("/products/popular")
  @Operation(summary = "인기 상품 조회", description = "최근 기간 동안 가장 많이 팔린 상품을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공")
  })
  public CommonResponse<List<PopularProductResponse>> getPopularProducts(
      @Parameter(description = "조회 기간 (일)", example = "3") @RequestParam(defaultValue = "3") int days,
      @Parameter(description = "조회 개수", example = "5") @RequestParam(defaultValue = "5") int limit) {

    // Mock 데이터 생성 (Record 생성자 사용)
    List<PopularProductResponse> products = List.of(
        new PopularProductResponse(
            1,
            1L,
            "고성능 노트북",
            new BigDecimal("1500000.00"),
            150,
            new BigDecimal("225000000.00")),
        new PopularProductResponse(
            2,
            2L,
            "무선 마우스",
            new BigDecimal("50000.00"),
            200,
            new BigDecimal("10000000.00")),
        new PopularProductResponse(
            3,
            3L,
            "기계식 키보드",
            new BigDecimal("150000.00"),
            120,
            new BigDecimal("18000000.00")),
        new PopularProductResponse(
            4,
            4L,
            "모니터",
            new BigDecimal("300000.00"),
            80,
            new BigDecimal("24000000.00")),
        new PopularProductResponse(
            5,
            5L,
            "웹캠",
            new BigDecimal("80000.00"),
            100,
            new BigDecimal("8000000.00")))
        .stream().limit(limit).toList();

    return CommonResponse.success(products);
  }
}