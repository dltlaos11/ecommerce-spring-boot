package kr.hhplus.be.server.coupon.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.coupon.application.GetCouponsUseCase;
import kr.hhplus.be.server.coupon.application.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.ValidateCouponUseCase;
import kr.hhplus.be.server.coupon.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * UseCase 패턴 최종 적용 - 검증 로직 분리
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/coupons")
@Tag(name = "쿠폰 관리", description = "쿠폰 발급, 조회 및 사용 관리 API")
@RequiredArgsConstructor
public class CouponController {

  private final GetCouponsUseCase getCouponsUseCase;
  private final IssueCouponUseCase issueCouponUseCase;
  private final ValidateCouponUseCase validateCouponUseCase; // 분리된 검증 UseCase

  /**
   * 발급 가능한 쿠폰 목록 조회
   */
  @GetMapping("/available")
  @Operation(summary = "발급 가능한 쿠폰 목록 조회", description = "현재 발급 가능한 모든 쿠폰의 목록을 조회합니다.")
  public CommonResponse<List<AvailableCouponResponse>> getAvailableCoupons() {
    log.info("발급 가능한 쿠폰 목록 조회 요청");

    List<AvailableCouponResponse> coupons = getCouponsUseCase.executeAvailableCoupons();

    return CommonResponse.success(coupons);
  }

  /**
   * 특정 쿠폰 조회
   */
  @GetMapping("/{couponId}")
  @Operation(summary = "쿠폰 상세 조회", description = "특정 쿠폰의 상세 정보를 조회합니다.")
  public CommonResponse<AvailableCouponResponse> getCoupon(
      @Parameter(description = "쿠폰 ID", example = "1", required = true) @PathVariable Long couponId) {

    log.info("쿠폰 상세 조회 요청: couponId = {}", couponId);

    AvailableCouponResponse coupon = getCouponsUseCase.executeCouponQuery(couponId);

    return CommonResponse.success(coupon);
  }

  /**
   * 쿠폰 발급
   */
  @PostMapping("/{couponId}/issue")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "쿠폰 발급", description = "사용자에게 쿠폰을 발급합니다. 중복 발급은 불가능합니다.")
  public CommonResponse<IssuedCouponResponse> issueCoupon(
      @Parameter(description = "쿠폰 ID", example = "1", required = true) @PathVariable Long couponId,
      @Valid @RequestBody IssueCouponRequest request) {

    log.info("쿠폰 발급 요청: couponId = {}, userId = {}", couponId, request.userId());

    IssuedCouponResponse response = issueCouponUseCase.execute(couponId, request.userId());

    return CommonResponse.success(response);
  }

  /**
   * 사용자 보유 쿠폰 목록 조회
   */
  @GetMapping("/users/{userId}")
  @Operation(summary = "사용자 보유 쿠폰 조회", description = "특정 사용자가 보유한 모든 쿠폰을 조회합니다.")
  public CommonResponse<List<UserCouponResponse>> getUserCoupons(
      @Parameter(description = "사용자 ID", example = "1", required = true) @PathVariable Long userId,
      @Parameter(description = "사용 가능한 쿠폰만 조회", example = "false") @RequestParam(defaultValue = "false") Boolean onlyAvailable) {

    log.info("사용자 쿠폰 목록 조회 요청: userId = {}, onlyAvailable = {}", userId, onlyAvailable);

    List<UserCouponResponse> userCoupons = onlyAvailable
        ? getCouponsUseCase.executeAvailableUserCoupons(userId)
        : getCouponsUseCase.executeUserCoupons(userId);

    return CommonResponse.success(userCoupons);
  }

  /**
   * 쿠폰 사용 가능 여부 검증 및 할인 금액 계산 - 분리된 UseCase 사용
   */
  @PostMapping("/validate")
  @Operation(summary = "쿠폰 검증 및 할인 계산", description = "쿠폰 사용 가능 여부를 확인하고 할인 금액을 계산합니다.")
  public CommonResponse<CouponValidationResponse> validateCoupon(@Valid @RequestBody CouponValidationRequest request) {
    log.info("쿠폰 검증 요청: userId = {}, couponId = {}, orderAmount = {}",
        request.userId(), request.couponId(), request.orderAmount());

    // 분리된 ValidateCouponUseCase 사용
    CouponValidationResponse response = validateCouponUseCase.execute(
        request.userId(), request.couponId(), request.orderAmount());

    return CommonResponse.success(response);
  }
}