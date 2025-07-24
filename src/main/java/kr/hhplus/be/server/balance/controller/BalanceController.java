package kr.hhplus.be.server.balance.controller;

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
import kr.hhplus.be.server.balance.dto.BalanceHistoryResponse;
import kr.hhplus.be.server.balance.dto.BalanceResponse;
import kr.hhplus.be.server.balance.dto.ChargeBalanceRequest;
import kr.hhplus.be.server.balance.dto.ChargeBalanceResponse;
import kr.hhplus.be.server.balance.service.BalanceService;
import kr.hhplus.be.server.common.response.CommonResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 잔액 컨트롤러
 * 
 * 변경사항:
 * - Mock 데이터 제거
 * - BalanceService 의존성 주입 및 실제 호출
 * - HTTP 요청/응답 처리에만 집중
 * 
 * 책임:
 * - HTTP 요청 파라미터 검증
 * - Service 호출 및 결과 반환
 * - 예외 처리는 @ControllerAdvice에 위임
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users/{userId}/balance")
@Tag(name = "잔액 관리", description = "사용자 잔액 충전, 조회 및 이력 관리 API")
// @CommonApiResponses // 각 API마다 개별 에러 응답 정의
public class BalanceController {

  private final BalanceService balanceService;

  public BalanceController(BalanceService balanceService) {
    this.balanceService = balanceService;
  }

  /**
   * 사용자 잔액 조회
   */
  @GetMapping
  @Operation(summary = "잔액 조회", description = "사용자의 현재 잔액을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "잔액 조회 성공", value = """
          {
            "success": true,
            "data": {
              "userId": 1,
              "balance": 50000.00,
              "lastUpdated": "2025-07-24T15:30:00"
            },
            "timestamp": "2025-07-24T15:30:00"
          }
          """))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "잘못된 파라미터", value = """
          {
            "success": false,
            "error": {
              "code": "INVALID_PARAMETER",
              "message": "잘못된 요청 파라미터입니다."
            },
            "timestamp": "2025-07-24T15:30:00"
          }
          """))),
      @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "사용자 없음", value = """
          {
            "success": false,
            "error": {
              "code": "USER_NOT_FOUND",
              "message": "사용자를 찾을 수 없습니다."
            },
            "timestamp": "2025-07-24T15:30:00"
          }
          """))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "서버 오류", value = """
          {
            "success": false,
            "error": {
              "code": "INTERNAL_ERROR",
              "message": "서버 내부 오류가 발생했습니다."
            },
            "timestamp": "2025-07-24T15:30:00"
          }
          """))),
  })
  public CommonResponse<BalanceResponse> getUserBalance(
      @Parameter(description = "사용자 ID", example = "1", required = true) @PathVariable Long userId) {

    log.info("💰 사용자 잔액 조회 요청: userId = {}", userId);

    BalanceResponse balance = balanceService.getUserBalance(userId);

    log.info("✅ 사용자 잔액 조회 완료: userId = {}, balance = {}",
        userId, balance.balance());

    return CommonResponse.success(balance);
  }

  /**
   * 잔액 충전
   */
  @PostMapping("/charge")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "잔액 충전", description = "사용자의 잔액을 충전합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "충전 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "잔액 충전 성공", value = """
          {
            "success": true,
            "data": {
              "userId": 1,
              "previousBalance": 20000.00,
              "chargedAmount": 30000.00,
              "currentBalance": 50000.00,
              "transactionId": "CHARGE_1721895000000_A1B2C3D4"
            },
            "timestamp": "2025-07-24T15:30:00"
          }
          """))),
      @ApiResponse(responseCode = "400", description = "잘못된 충전 요청", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "금액 범위 오류", value = """
              {
                "success": false,
                "error": {
                  "code": "INVALID_CHARGE_AMOUNT",
                  "message": "충전 금액이 올바르지 않습니다."
                },
                "timestamp": "2025-07-24T15:30:00"
              }
              """),
          @ExampleObject(name = "한도 초과", value = """
              {
                "success": false,
                "error": {
                  "code": "MAX_BALANCE_LIMIT_EXCEEDED",
                  "message": "최대 보유 가능 잔액을 초과했습니다."
                },
                "timestamp": "2025-07-24T15:30:00"
              }
              """)
      })),
      @ApiResponse(responseCode = "409", description = "동시성 충돌", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "동시성 충돌", value = """
          {
            "success": false,
            "error": {
              "code": "CONFLICT",
              "message": "요청 처리 중 충돌이 발생했습니다."
            },
            "timestamp": "2025-07-24T15:30:00"
          }
          """)))
  })
  public CommonResponse<ChargeBalanceResponse> chargeBalance(
      @Parameter(description = "사용자 ID", example = "1", required = true) @PathVariable Long userId,

      @Valid @RequestBody ChargeBalanceRequest request) {

    log.info("💳 잔액 충전 요청: userId = {}, amount = {}", userId, request.amount());

    ChargeBalanceResponse response = balanceService.chargeBalance(userId, request.amount());

    log.info("✅ 잔액 충전 완료: userId = {}, 충전 금액 = {}, 현재 잔액 = {}",
        userId, response.chargedAmount(), response.currentBalance());

    return CommonResponse.success(response);
  }

  /**
   * 잔액 변동 이력 조회
   */
  @GetMapping("/history")
  @Operation(summary = "잔액 변동 이력 조회", description = "사용자의 잔액 변동 이력을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "이력 조회 성공", value = """
          {
            "success": true,
            "data": [
              {
                "transactionType": "CHARGE",
                "amount": 30000.00,
                "balanceAfter": 50000.00,
                "createdAt": "2025-07-24T15:30:00"
              },
              {
                "transactionType": "PAYMENT",
                "amount": 15000.00,
                "balanceAfter": 20000.00,
                "createdAt": "2025-07-24T14:00:00"
              }
            ],
            "timestamp": "2025-07-24T15:30:00"
          }
          """)))
  })
  public CommonResponse<List<BalanceHistoryResponse>> getBalanceHistory(
      @Parameter(description = "사용자 ID", example = "1", required = true) @PathVariable Long userId,

      @Parameter(description = "조회할 이력 개수", example = "10") @RequestParam(defaultValue = "10") int limit) {

    log.info("📋 잔액 이력 조회 요청: userId = {}, limit = {}", userId, limit);

    List<BalanceHistoryResponse> histories = balanceService.getBalanceHistories(userId, limit);

    log.info("✅ 잔액 이력 조회 완료: userId = {}, {}개 이력", userId, histories.size());

    return CommonResponse.success(histories);
  }

  /**
   * 잔액 충분 여부 확인 (내부 API)
   */
  @GetMapping("/check")
  @Operation(summary = "잔액 충분 여부 확인", description = "특정 금액에 대해 잔액이 충분한지 확인합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "확인 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "잔액 확인 성공", value = """
          {
            "success": true,
            "data": {
              "userId": 1,
              "requestedAmount": 25000.00,
              "currentBalance": 50000.00,
              "sufficient": true
            },
            "timestamp": "2025-07-24T15:30:00"
          }
          """)))
  })
  public CommonResponse<BalanceCheckResponse> checkBalance(
      @Parameter(description = "사용자 ID", example = "1", required = true) @PathVariable Long userId,

      @Parameter(description = "확인할 금액", example = "25000.00", required = true) @RequestParam java.math.BigDecimal amount) {

    log.info("💳 잔액 확인 요청: userId = {}, amount = {}", userId, amount);

    BalanceResponse balance = balanceService.getUserBalance(userId);
    boolean sufficient = balanceService.hasEnoughBalance(userId, amount);

    BalanceCheckResponse response = new BalanceCheckResponse(
        userId,
        amount,
        balance.balance(),
        sufficient);

    log.info("✅ 잔액 확인 완료: userId = {}, 결과 = {}", userId, sufficient ? "충분" : "부족");

    return CommonResponse.success(response);
  }

  /**
   * 잔액 확인 응답 DTO
   */
  public record BalanceCheckResponse(
      Long userId,
      java.math.BigDecimal requestedAmount,
      java.math.BigDecimal currentBalance,
      Boolean sufficient) {
  }
}