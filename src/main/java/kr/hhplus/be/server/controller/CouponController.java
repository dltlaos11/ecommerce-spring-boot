package kr.hhplus.be.server.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.dto.coupon.AvailableCouponResponse;
import kr.hhplus.be.server.dto.coupon.IssueCouponRequest;
import kr.hhplus.be.server.dto.coupon.IssuedCouponResponse;

@RestController
@RequestMapping("/api/v1/coupons")
@Tag(name = "쿠폰 관리", description = "쿠폰 발급 및 조회 API")
public class CouponController {

  @PostMapping("/{couponId}/issue")
  @Operation(summary = "선착순 쿠폰 발급", description = "선착순으로 쿠폰을 발급받습니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "발급 성공", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = """
          {
            "success": true,
            "data": {
              "userCouponId": 123,
              "couponId": 1,
              "userId": 1,
              "couponName": "10% 할인 쿠폰",
              "discountType": "PERCENTAGE",
              "discountValue": 10.00,
              "expiredAt": "2025-07-24T10:30:00",
              "issuedAt": "2025-07-17T10:30:00",
              "status": "AVAILABLE"
            },
            "error": null,
            "timestamp": "2025-07-17T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "404", description = "쿠폰을 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = """
          {
            "success": false,
            "data": null,
            "error": {
              "code": "COUPON_NOT_FOUND",
              "message": "쿠폰을 찾을 수 없습니다."
            },
            "timestamp": "2025-07-17T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "409", description = "이미 발급받았거나 품절", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = """
          {
            "success": false,
            "data": null,
            "error": {
              "code": "COUPON_ALREADY_ISSUED",
              "message": "이미 발급받은 쿠폰이거나 품절되었습니다."
            },
            "timestamp": "2025-07-17T10:30:00"
          }
          """)))
  })
  @ResponseStatus(HttpStatus.CREATED)
  public CommonResponse<IssuedCouponResponse> issueCoupon(
      @Parameter(description = "쿠폰 ID", example = "1") @PathVariable Long couponId,
      @Valid @RequestBody IssueCouponRequest request) {

    // Mock 데이터 생성
    IssuedCouponResponse response = new IssuedCouponResponse(
        System.currentTimeMillis(), // 고유 ID 생성
        couponId,
        request.getUserId(),
        getCouponName(couponId),
        getCouponDiscountType(couponId),
        getCouponDiscountValue(couponId),
        LocalDateTime.now().plusDays(7), // 7일 후 만료
        LocalDateTime.now(),
        "AVAILABLE" // status
    );

    return CommonResponse.success(response);
  }

  @GetMapping("/available")
  @Operation(summary = "발급 가능한 쿠폰 목록 조회", description = "현재 발급 가능한 쿠폰 목록을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = """
          {
            "success": true,
            "data": [
              {
                "id": 1,
                "name": "10% 할인 쿠폰",
                "discountType": "PERCENTAGE",
                "discountValue": 10.00,
                "maxDiscountAmount": 50000.00,
                "minimumOrderAmount": 100000.00,
                "remainingQuantity": 95,
                "totalQuantity": 100,
                "expiredAt": "2025-07-24T23:59:59"
              },
              {
                "id": 2,
                "name": "5000원 할인 쿠폰",
                "discountType": "FIXED",
                "discountValue": 5000.00,
                "maxDiscountAmount": null,
                "minimumOrderAmount": 50000.00,
                "remainingQuantity": 150,
                "totalQuantity": 200,
                "expiredAt": "2025-07-27T23:59:59"
              }
            ],
            "error": null,
            "timestamp": "2025-07-17T10:30:00"
          }
          """)))
  })
  public CommonResponse<List<AvailableCouponResponse>> getAvailableCoupons() {

    // Mock 데이터 생성 (minimumOrderAmount 필드 추가)
    List<AvailableCouponResponse> coupons = List.of(
        new AvailableCouponResponse(
            1L,
            "10% 할인 쿠폰",
            "PERCENTAGE",
            new BigDecimal("10.00"),
            new BigDecimal("50000.00"),
            new BigDecimal("100000.00"), // 최소 주문 금액 추가
            95,
            100,
            LocalDateTime.now().plusDays(7)),
        new AvailableCouponResponse(
            2L,
            "5000원 할인 쿠폰",
            "FIXED",
            new BigDecimal("5000.00"),
            null,
            new BigDecimal("50000.00"), // 최소 주문 금액 추가
            150,
            200,
            LocalDateTime.now().plusDays(10)),
        new AvailableCouponResponse(
            3L,
            "15% 할인 쿠폰",
            "PERCENTAGE",
            new BigDecimal("15.00"),
            new BigDecimal("100000.00"),
            new BigDecimal("200000.00"), // 최소 주문 금액 추가
            25,
            50,
            LocalDateTime.now().plusDays(14)));

    return CommonResponse.success(coupons);
  }

  @GetMapping("/users/{userId}")
  @Operation(summary = "사용자 보유 쿠폰 조회", description = "사용자가 보유한 쿠폰 목록을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = """
          {
            "success": true,
            "data": [
              {
                "userCouponId": 123,
                "couponId": 1,
                "userId": 1,
                "couponName": "10% 할인 쿠폰",
                "discountType": "PERCENTAGE",
                "discountValue": 10.00,
                "expiredAt": "2025-07-24T10:30:00",
                "issuedAt": "2025-07-16T10:30:00",
                "status": "AVAILABLE"
              },
              {
                "userCouponId": 124,
                "couponId": 2,
                "userId": 1,
                "couponName": "5000원 할인 쿠폰",
                "discountType": "FIXED",
                "discountValue": 5000.00,
                "expiredAt": "2025-07-27T10:30:00",
                "issuedAt": "2025-07-15T10:30:00",
                "status": "USED"
              }
            ],
            "error": null,
            "timestamp": "2025-07-17T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = """
          {
            "success": false,
            "data": null,
            "error": {
              "code": "USER_NOT_FOUND",
              "message": "사용자를 찾을 수 없습니다."
            },
            "timestamp": "2025-07-17T10:30:00"
          }
          """)))
  })
  public CommonResponse<List<IssuedCouponResponse>> getUserCoupons(
      @Parameter(description = "사용자 ID", example = "1") @PathVariable Long userId,
      @Parameter(description = "쿠폰 상태 필터", example = "AVAILABLE") @RequestParam(required = false) @Schema(allowableValues = {
          "AVAILABLE", "USED", "EXPIRED" }) String status) {

    // 전체 Mock 데이터 생성
    List<IssuedCouponResponse> allCoupons = List.of(
        new IssuedCouponResponse(
            123L,
            1L,
            userId,
            "10% 할인 쿠폰",
            "PERCENTAGE",
            new BigDecimal("10.00"),
            LocalDateTime.now().plusDays(7),
            LocalDateTime.now().minusDays(1),
            "AVAILABLE"),
        new IssuedCouponResponse(
            124L,
            2L,
            userId,
            "5000원 할인 쿠폰",
            "FIXED",
            new BigDecimal("5000.00"),
            LocalDateTime.now().plusDays(10),
            LocalDateTime.now().minusDays(2),
            "USED"),
        new IssuedCouponResponse(
            125L,
            3L,
            userId,
            "15% 할인 쿠폰",
            "PERCENTAGE",
            new BigDecimal("15.00"),
            LocalDateTime.now().minusDays(1), // 만료된 쿠폰
            LocalDateTime.now().minusDays(5),
            "EXPIRED"));

    // status 파라미터로 필터링 적용
    List<IssuedCouponResponse> filteredCoupons = allCoupons;
    if (status != null && !status.trim().isEmpty()) {
      filteredCoupons = allCoupons.stream()
          .filter(coupon -> status.equalsIgnoreCase(coupon.getStatus()))
          .toList();
    }

    return CommonResponse.success(filteredCoupons);
  }

  // Mock 헬퍼 메서드들
  private String getCouponName(Long couponId) {
    return switch (couponId.intValue()) {
      case 1 -> "10% 할인 쿠폰";
      case 2 -> "5000원 할인 쿠폰";
      case 3 -> "15% 할인 쿠폰";
      default -> "할인 쿠폰";
    };
  }

  private String getCouponDiscountType(Long couponId) {
    return switch (couponId.intValue()) {
      case 1, 3 -> "PERCENTAGE";
      case 2 -> "FIXED";
      default -> "PERCENTAGE";
    };
  }

  private BigDecimal getCouponDiscountValue(Long couponId) {
    return switch (couponId.intValue()) {
      case 1 -> new BigDecimal("10.00");
      case 2 -> new BigDecimal("5000.00");
      case 3 -> new BigDecimal("15.00");
      default -> new BigDecimal("10.00");
    };
  }
}