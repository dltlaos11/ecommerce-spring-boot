package kr.hhplus.be.server.coupon.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.coupon.dto.*;
import kr.hhplus.be.server.coupon.service.CouponService;
import lombok.extern.slf4j.Slf4j;

/**
 * 쿠폰 컨트롤러
 * 
 * 변경사항:
 * - 실제 CouponService 연동
 * - HTTP 요청/응답 처리에만 집중
 * - 비즈니스 로직은 Service에 위임
 * 
 * 책임:
 * - HTTP 요청 파라미터 검증
 * - Service 호출 및 결과 반환
 * - 예외 처리는 GlobalExceptionHandler에 위임
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/coupons")
@Tag(name = "쿠폰 관리", description = "쿠폰 발급, 조회 및 사용 관리 API")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * 발급 가능한 쿠폰 목록 조회
     */
    @GetMapping("/available")
    @Operation(summary = "발급 가능한 쿠폰 목록 조회", description = "현재 발급 가능한 모든 쿠폰의 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "발급 가능한 쿠폰 목록", value = """
                    {
                      "success": true,
                      "data": [
                        {
                          "id": 1,
                          "name": "신규 가입 쿠폰",
                          "discountType": "FIXED",
                          "discountValue": 5000.00,
                          "maxDiscountAmount": 5000.00,
                          "minimumOrderAmount": 30000.00,
                          "remainingQuantity": 95,
                          "totalQuantity": 100,
                          "expiredAt": "2025-08-24T23:59:59"
                        }
                      ],
                      "timestamp": "2025-07-25T10:30:00"
                    }
                    """))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "서버 오류", value = """
                    {
                      "success": false,
                      "error": {
                        "code": "INTERNAL_ERROR",
                        "message": "서버 내부 오류가 발생했습니다."
                      },
                      "timestamp": "2025-07-25T10:30:00"
                    }
                    """)))
    })
    public CommonResponse<List<AvailableCouponResponse>> getAvailableCoupons() {
        log.info("🎫 발급 가능한 쿠폰 목록 조회 요청");

        List<AvailableCouponResponse> coupons = couponService.getAvailableCoupons();

        log.info("✅ 발급 가능한 쿠폰 목록 조회 완료: {}개", coupons.size());

        return CommonResponse.success(coupons);
    }

    /**
     * 특정 쿠폰 조회
     */
    @GetMapping("/{couponId}")
    @Operation(summary = "쿠폰 상세 조회", description = "특정 쿠폰의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "쿠폰 상세 정보", value = """
                    {
                      "success": true,
                      "data": {
                        "id": 1,
                        "name": "VIP 10% 할인 쿠폰",
                        "discountType": "PERCENTAGE",
                        "discountValue": 10.00,
                        "maxDiscountAmount": 50000.00,
                        "minimumOrderAmount": 100000.00,
                        "remainingQuantity": 45,
                        "totalQuantity": 50,
                        "expiredAt": "2025-08-01T23:59:59"
                      },
                      "timestamp": "2025-07-25T10:30:00"
                    }
                    """))),
            @ApiResponse(responseCode = "404", description = "쿠폰을 찾을 수 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "쿠폰 없음", value = """
                    {
                      "success": false,
                      "error": {
                        "code": "COUPON_NOT_FOUND",
                        "message": "쿠폰을 찾을 수 없습니다."
                      },
                      "timestamp": "2025-07-25T10:30:00"
                    }
                    """)))
    })
    public CommonResponse<AvailableCouponResponse> getCoupon(
            @Parameter(description = "쿠폰 ID", example = "1", required = true) @PathVariable Long couponId) {

        log.info("🔍 쿠폰 상세 조회 요청: couponId = {}", couponId);

        AvailableCouponResponse coupon = couponService.getCoupon(couponId);

        log.info("✅ 쿠폰 상세 조회 완료: couponId = {}, 이름 = '{}'", couponId, coupon.name());

        return CommonResponse.success(coupon);
    }

    /**
     * 쿠폰 발급
     */
    @PostMapping("/{couponId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "쿠폰 발급", description = "사용자에게 쿠폰을 발급합니다. 중복 발급은 불가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "발급 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "쿠폰 발급 성공", value = """
                    {
                      "success": true,
                      "data": {
                        "userCouponId": 123,
                        "couponId": 1,
                        "userId": 1,
                        "couponName": "신규 가입 쿠폰",
                        "discountType": "FIXED",
                        "discountValue": 5000.00,
                        "maxDiscountAmount": 5000.00,
                        "minimumOrderAmount": 30000.00,
                        "expiredAt": "2025-08-24T23:59:59",
                        "issuedAt": "2025-07-25T10:30:00",
                        "status": "AVAILABLE"
                      },
                      "timestamp": "2025-07-25T10:30:00"
                    }
                    """))),
            @ApiResponse(responseCode = "400", description = "잘못된 발급 요청", content = @Content(mediaType = "application/json", examples = {
                    @ExampleObject(name = "잘못된 요청", value = """
                            {
                              "success": false,
                              "error": {
                                "code": "VALIDATION_ERROR",
                                "message": "사용자 ID는 필수입니다."
                              },
                              "timestamp": "2025-07-25T10:30:00"
                            }
                            """)
            })),
            @ApiResponse(responseCode = "404", description = "쿠폰을 찾을 수 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "쿠폰 없음", value = """
                    {
                      "success": false,
                      "error": {
                        "code": "COUPON_NOT_FOUND",
                        "message": "쿠폰을 찾을 수 없습니다."
                      },
                      "timestamp": "2025-07-25T10:30:00"
                    }
                    """))),
            @ApiResponse(responseCode = "409", description = "쿠폰 발급 불가", content = @Content(mediaType = "application/json", examples = {
                    @ExampleObject(name = "이미 발급됨", value = """
                            {
                              "success": false,
                              "error": {
                                "code": "COUPON_ALREADY_ISSUED",
                                "message": "이미 발급받은 쿠폰입니다."
                              },
                              "timestamp": "2025-07-25T10:30:00"
                            }
                            """),
                    @ExampleObject(name = "쿠폰 소진", value = """
                            {
                              "success": false,
                              "error": {
                                "code": "COUPON_EXHAUSTED",
                                "message": "선착순 쿠폰이 모두 소진되었습니다."
                              },
                              "timestamp": "2025-07-25T10:30:00"
                            }
                            """),
                    @ExampleObject(name = "쿠폰 만료", value = """
                            {
                              "success": false,
                              "error": {
                                "code": "COUPON_EXPIRED",
                                "message": "쿠폰이 만료되었습니다."
                              },
                              "timestamp": "2025-07-25T10:30:00"
                            }
                            """)
            }))
    })
    public CommonResponse<IssuedCouponResponse> issueCoupon(
            @Parameter(description = "쿠폰 ID", example = "1", required = true) @PathVariable Long couponId,

            @Valid @RequestBody IssueCouponRequest request) {

        log.info("🎫 쿠폰 발급 요청: couponId = {}, userId = {}", couponId, request.userId());

        IssuedCouponResponse response = couponService.issueCoupon(couponId, request.userId());

        log.info("✅ 쿠폰 발급 완료: couponId = {}, userId = {}", couponId, request.userId());

        return CommonResponse.success(response);
    }

    /**
     * 사용자 보유 쿠폰 목록 조회
     */
    @GetMapping("/users/{userId}")
    @Operation(summary = "사용자 보유 쿠폰 조회", description = "특정 사용자가 보유한 모든 쿠폰을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "사용자 쿠폰 목록", value = """
                    {
                      "success": true,
                      "data": [
                        {
                          "userCouponId": 123,
                          "couponId": 1,
                          "couponName": "신규 가입 쿠폰",
                          "discountType": "FIXED",
                          "discountValue": 5000.00,
                          "maxDiscountAmount": 5000.00,
                          "minimumOrderAmount": 30000.00,
                          "status": "AVAILABLE",
                          "expiredAt": "2025-08-24T23:59:59",
                          "issuedAt": "2025-07-25T10:30:00",
                          "usedAt": null
                        }
                      ],
                      "timestamp": "2025-07-25T10:30:00"
                    }
                    """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "잘못된 사용자 ID", value = """
                    {
                      "success": false,
                      "error": {
                        "code": "INVALID_PARAMETER",
                        "message": "잘못된 요청 파라미터입니다."
                      },
                      "timestamp": "2025-07-25T10:30:00"
                    }
                    """)))
    })
    public CommonResponse<List<UserCouponResponse>> getUserCoupons(
            @Parameter(description = "사용자 ID", example = "1", required = true) @PathVariable Long userId,

            @Parameter(description = "사용 가능한 쿠폰만 조회", example = "false") @RequestParam(defaultValue = "false") Boolean onlyAvailable) {

        log.info("📋 사용자 쿠폰 목록 조회 요청: userId = {}, onlyAvailable = {}", userId, onlyAvailable);

        List<UserCouponResponse> userCoupons;
        if (onlyAvailable) {
            userCoupons = couponService.getAvailableUserCoupons(userId);
        } else {
            userCoupons = couponService.getUserCoupons(userId);
        }

        log.info("✅ 사용자 쿠폰 목록 조회 완료: userId = {}, {}개 쿠폰", userId, userCoupons.size());

        return CommonResponse.success(userCoupons);
    }

    /**
     * 쿠폰 사용 가능 여부 검증 및 할인 금액 계산
     */
    @PostMapping("/validate")
    @Operation(summary = "쿠폰 검증 및 할인 계산", description = "쿠폰 사용 가능 여부를 확인하고 할인 금액을 계산합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검증 완료", content = @Content(mediaType = "application/json", examples = {
                    @ExampleObject(name = "사용 가능", value = """
                            {
                              "success": true,
                              "data": {
                                "couponId": 1,
                                "userId": 1,
                                "usable": true,
                                "discountAmount": 15000.00,
                                "finalAmount": 135000.00,
                                "reason": null
                              },
                              "timestamp": "2025-07-25T10:30:00"
                            }
                            """),
                    @ExampleObject(name = "사용 불가", value = """
                            {
                              "success": true,
                              "data": {
                                "couponId": 1,
                                "userId": 1,
                                "usable": false,
                                "discountAmount": 0.00,
                                "finalAmount": 150000.00,
                                "reason": "최소 주문 금액 100000원 이상이어야 합니다."
                              },
                              "timestamp": "2025-07-25T10:30:00"
                            }
                            """)
            })),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "검증 실패", value = """
                    {
                      "success": false,
                      "error": {
                        "code": "VALIDATION_ERROR",
                        "message": "주문 금액은 0보다 커야 합니다."
                      },
                      "timestamp": "2025-07-25T10:30:00"
                    }
                    """)))
    })
    public CommonResponse<CouponValidationResponse> validateCoupon(
            @Valid @RequestBody CouponValidationRequest request) {

        log.info("🧮 쿠폰 검증 요청: userId = {}, couponId = {}, orderAmount = {}",
                request.userId(), request.couponId(), request.orderAmount());

        CouponValidationResponse response = couponService.validateAndCalculateDiscount(
                request.userId(), request.couponId(), request.orderAmount());

        log.info("✅ 쿠폰 검증 완료: usable = {}, discountAmount = {}",
                response.usable(), response.discountAmount());

        return CommonResponse.success(response);
    }
}