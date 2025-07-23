package kr.hhplus.be.server.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

  /**
   * Mock 데이터: 사용자별 발급받은 쿠폰 추적
   * 실제 구현에서는 데이터베이스에서 관리
   */
  private final ConcurrentHashMap<String, Boolean> issuedCoupons = new ConcurrentHashMap<>();
  private final AtomicInteger couponCount = new AtomicInteger(95); // 초기 남은 수량

  @PostMapping("/{couponId}/issue")
  @Operation(summary = "선착순 쿠폰 발급", description = "선착순으로 쿠폰을 발급받습니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "발급 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(name = "쿠폰 발급 성공", value = """
          {
            "success": true,
            "data": {
              "userCouponId": 1721480000000,
              "couponId": 1,
              "userId": 1,
              "couponName": "10% 할인 쿠폰",
              "discountType": "PERCENTAGE",
              "discountValue": 10.00,
              "expiredAt": "2025-07-27T10:30:00",
              "issuedAt": "2025-07-20T10:30:00",
              "status": "AVAILABLE"
            },
            "timestamp": "2025-07-20T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "404", description = "쿠폰을 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(name = "쿠폰 없음", value = """
          {
            "success": false,
            "error": {
              "code": "COUPON_NOT_FOUND",
              "message": "쿠폰을 찾을 수 없습니다."
            },
            "timestamp": "2025-07-20T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "409", description = "이미 발급받았거나 품절", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = {
          @ExampleObject(name = "중복 발급", value = """
              {
                "success": false,
                "error": {
                  "code": "COUPON_ALREADY_ISSUED",
                  "message": "이미 발급받은 쿠폰입니다."
                },
                "timestamp": "2025-07-20T10:30:00"
              }
              """),
          @ExampleObject(name = "쿠폰 품절", value = """
              {
                "success": false,
                "error": {
                  "code": "COUPON_EXHAUSTED",
                  "message": "선착순 쿠폰이 모두 소진되었습니다."
                },
                "timestamp": "2025-07-20T10:30:00"
              }
              """)
      }))
  })
  public CommonResponse<IssuedCouponResponse> issueCoupon(
      @Parameter(description = "쿠폰 ID", example = "1") @PathVariable Long couponId,
      @Valid @RequestBody IssueCouponRequest request) {

    // 1. 쿠폰 존재 여부 확인 (404 에러 시뮬레이션)
    if (couponId > 3) {
      return CommonResponse.error("COUPON_NOT_FOUND", "쿠폰을 찾을 수 없습니다.");
    }

    // 2. 쿠폰 품절 확인 (409 에러 시뮬레이션)
    if (couponCount.get() <= 0) {
      return CommonResponse.error("COUPON_EXHAUSTED", "선착순 쿠폰이 모두 소진되었습니다.");
    }

    // 3. 중복 발급 확인 (409 에러 시뮬레이션)
    String userCouponKey = request.userId() + "_" + couponId;
    if (issuedCoupons.containsKey(userCouponKey)) {
      return CommonResponse.error("COUPON_ALREADY_ISSUED", "이미 발급받은 쿠폰입니다.");
    }

    // 4. 쿠폰 발급 처리 (성공 케이스)
    issuedCoupons.put(userCouponKey, true);
    couponCount.decrementAndGet();

    IssuedCouponResponse response = new IssuedCouponResponse(
        System.currentTimeMillis(),
        couponId,
        request.userId(),
        getCouponName(couponId),
        getCouponDiscountType(couponId),
        getCouponDiscountValue(couponId),
        LocalDateTime.now().plusDays(7),
        LocalDateTime.now(),
        "AVAILABLE");

    return CommonResponse.success(response);
  }

  @GetMapping("/available")
  @Operation(summary = "발급 가능한 쿠폰 목록 조회", description = "현재 발급 가능한 쿠폰 목록을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공")
  })
  public CommonResponse<List<AvailableCouponResponse>> getAvailableCoupons() {

    List<AvailableCouponResponse> coupons = List.of(
        new AvailableCouponResponse(
            1L,
            "10% 할인 쿠폰",
            "PERCENTAGE",
            new BigDecimal("10.00"),
            new BigDecimal("50000.00"),
            new BigDecimal("100000.00"),
            couponCount.get(), // 실시간 수량 반영
            100,
            LocalDateTime.now().plusDays(7)),
        new AvailableCouponResponse(
            2L,
            "5000원 할인 쿠폰",
            "FIXED",
            new BigDecimal("5000.00"),
            null,
            new BigDecimal("50000.00"),
            150,
            200,
            LocalDateTime.now().plusDays(10)),
        new AvailableCouponResponse(
            3L,
            "15% 할인 쿠폰",
            "PERCENTAGE",
            new BigDecimal("15.00"),
            new BigDecimal("100000.00"),
            new BigDecimal("200000.00"),
            25,
            50,
            LocalDateTime.now().plusDays(14)));

    return CommonResponse.success(coupons);
  }

  @GetMapping("/users/{userId}")
  @Operation(summary = "사용자 보유 쿠폰 조회", description = "사용자가 보유한 쿠폰 목록을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(name = "사용자 없음", value = """
          {
            "success": false,
            "error": {
              "code": "USER_NOT_FOUND",
              "message": "사용자를 찾을 수 없습니다."
            },
            "timestamp": "2025-07-20T10:30:00"
          }
          """)))
  })
  public CommonResponse<List<IssuedCouponResponse>> getUserCoupons(
      @Parameter(description = "사용자 ID", example = "1") @PathVariable Long userId,
      @Parameter(description = "쿠폰 상태 필터", example = "AVAILABLE") @RequestParam(required = false) String status) {

    // 사용자 존재 여부 확인 (404 에러 시뮬레이션)
    if (userId > 100) {
      return CommonResponse.error("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.");
    }

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
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(5),
            "EXPIRED"));

    List<IssuedCouponResponse> filteredCoupons = allCoupons;
    if (status != null && !status.trim().isEmpty()) {
      filteredCoupons = allCoupons.stream()
          .filter(coupon -> status.equalsIgnoreCase(coupon.status()))
          .toList();
    }

    return CommonResponse.success(filteredCoupons);
  }

  /**
   * 쿠폰 수량 리셋 (테스트용)
   */
  @PostMapping("/reset")
  @Operation(summary = "쿠폰 수량 리셋", description = "테스트를 위해 쿠폰 수량을 리셋합니다.")
  public CommonResponse<String> resetCoupons() {
    issuedCoupons.clear();
    couponCount.set(95);
    return CommonResponse.success("쿠폰 수량이 리셋되었습니다.");
  }

  /**
   * Mock 헬퍼 메서드들
   * 
   * 실제 구현 시에는 CouponService에서 데이터베이스로부터 조회한다.
   */
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