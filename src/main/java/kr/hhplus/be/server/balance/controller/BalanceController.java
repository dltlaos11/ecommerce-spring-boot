package kr.hhplus.be.server.balance.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.balance.application.BalanceUseCase; // UseCase 의존성 주입
import kr.hhplus.be.server.balance.dto.BalanceHistoryResponse;
import kr.hhplus.be.server.balance.dto.BalanceResponse;
import kr.hhplus.be.server.balance.dto.ChargeBalanceRequest;
import kr.hhplus.be.server.balance.dto.ChargeBalanceResponse;
import kr.hhplus.be.server.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Application Layer 적용
 * 변경사항:
 * - BalanceService → BalanceUseCase 의존성 변경
 * - HTTP 요청/응답 처리에만 집중
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users/{userId}/balance")
@Tag(name = "잔액 관리", description = "사용자 잔액 충전, 조회 및 이력 관리 API")
@RequiredArgsConstructor
public class BalanceController {

  private final BalanceUseCase balanceUseCase; // UseCase 의존성 주입

  /**
   * 사용자 잔액 조회
   */
  @GetMapping
  @Operation(summary = "잔액 조회", description = "사용자의 현재 잔액을 조회합니다.")
  public CommonResponse<BalanceResponse> getUserBalance(
      @Parameter(description = "사용자 ID", example = "1", required = true) @PathVariable Long userId) {

    log.info("💰 사용자 잔액 조회 요청: userId = {}", userId);

    BalanceResponse balance = balanceUseCase.getUserBalance(userId);

    log.info("✅ 사용자 잔액 조회 완료: userId = {}, balance = {}", userId, balance.balance());

    return CommonResponse.success(balance);
  }

  /**
   * 잔액 충전
   */
  @PostMapping("/charge")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "잔액 충전", description = "사용자의 잔액을 충전합니다.")
  public CommonResponse<ChargeBalanceResponse> chargeBalance(
      @Parameter(description = "사용자 ID", example = "1", required = true) @PathVariable Long userId,
      @Valid @RequestBody ChargeBalanceRequest request) {

    log.info("💳 잔액 충전 요청: userId = {}, amount = {}", userId, request.amount());

    ChargeBalanceResponse response = balanceUseCase.chargeBalance(userId, request.amount());

    log.info("✅ 잔액 충전 완료: userId = {}, 충전 금액 = {}, 현재 잔액 = {}",
        userId, response.chargedAmount(), response.currentBalance());

    return CommonResponse.success(response);
  }

  /**
   * 잔액 변동 이력 조회
   */
  @GetMapping("/history")
  @Operation(summary = "잔액 변동 이력 조회", description = "사용자의 잔액 변동 이력을 조회합니다.")
  public CommonResponse<List<BalanceHistoryResponse>> getBalanceHistory(
      @Parameter(description = "사용자 ID", example = "1", required = true) @PathVariable Long userId,
      @Parameter(description = "조회할 이력 개수", example = "10") @RequestParam(defaultValue = "10") int limit) {

    log.info("📋 잔액 이력 조회 요청: userId = {}, limit = {}", userId, limit);

    List<BalanceHistoryResponse> histories = balanceUseCase.getBalanceHistories(userId, limit);

    log.info("✅ 잔액 이력 조회 완료: userId = {}, {}개 이력", userId, histories.size());

    return CommonResponse.success(histories);
  }
}
