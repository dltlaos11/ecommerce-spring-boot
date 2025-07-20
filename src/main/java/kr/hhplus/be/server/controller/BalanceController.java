package kr.hhplus.be.server.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.controller.spec.BalanceApiSpec;
import kr.hhplus.be.server.dto.balance.BalanceHistoryResponse;
import kr.hhplus.be.server.dto.balance.BalanceResponse;
import kr.hhplus.be.server.dto.balance.ChargeBalanceRequest;
import kr.hhplus.be.server.dto.balance.ChargeBalanceResponse;

@RestController
@RequestMapping("/api/v1/users")
public class BalanceController implements BalanceApiSpec {

  @GetMapping("/{userId}/balance")
  @Override
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(name = "잔액 조회 성공", value = """
          {
            "success": true,
            "data": {
              "userId": 1,
              "balance": 50000.00,
              "lastUpdated": "2025-07-20T10:30:00"
            },
            "timestamp": "2025-07-20T10:30:00"
          }
          """))),
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
  public CommonResponse<BalanceResponse> getBalance(@PathVariable Long userId) {
    BalanceResponse balance = new BalanceResponse(
        userId,
        new BigDecimal("50000.00"),
        LocalDateTime.now());
    return CommonResponse.success(balance);
  }

  @PostMapping("/{userId}/balance/charge")
  @Override
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "충전 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(name = "잔액 충전 성공", value = """
          {
            "success": true,
            "data": {
              "userId": 1,
              "previousBalance": 50000.00,
              "chargedAmount": 30000.00,
              "currentBalance": 80000.00,
              "transactionId": "TXN_1721124600000_A1B2C3D4"
            },
            "timestamp": "2025-07-20T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "400", description = "잘못된 충전 금액", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(name = "잘못된 금액", value = """
          {
            "success": false,
            "error": {
              "code": "INVALID_CHARGE_AMOUNT",
              "message": "충전 금액이 올바르지 않습니다."
            },
            "timestamp": "2025-07-20T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "409", description = "동시성 충돌", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(name = "동시성 충돌", value = """
          {
            "success": false,
            "error": {
              "code": "BALANCE_CONCURRENCY_ERROR",
              "message": "동시 처리로 인한 충돌이 발생했습니다. 다시 시도해주세요."
            },
            "timestamp": "2025-07-20T10:30:00"
          }
          """)))
  })
  public CommonResponse<ChargeBalanceResponse> chargeBalance(
      @PathVariable Long userId,
      @Valid @RequestBody ChargeBalanceRequest request) {

    BigDecimal previousBalance = new BigDecimal("50000.00");
    BigDecimal currentBalance = previousBalance.add(request.amount());
    String transactionId = generateTransactionId();

    ChargeBalanceResponse response = new ChargeBalanceResponse(
        userId,
        previousBalance,
        request.amount(),
        currentBalance,
        transactionId);
    return CommonResponse.success(response);
  }

  @GetMapping("/{userId}/balance/history")
  @Override
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(name = "잔액 이력 조회 성공", value = """
          {
            "success": true,
            "data": [
              {
                "transactionType": "CHARGE",
                "amount": 50000.00,
                "balanceAfter": 100000.00,
                "createdAt": "2025-07-19T10:30:00"
              },
              {
                "transactionType": "PAYMENT",
                "amount": -30000.00,
                "balanceAfter": 70000.00,
                "createdAt": "2025-07-19T15:30:00"
              }
            ],
            "timestamp": "2025-07-20T10:30:00"
          }
          """))),
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
  public CommonResponse<List<BalanceHistoryResponse>> getBalanceHistory(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "10") int limit) {

    List<BalanceHistoryResponse> history = List.of(
        new BalanceHistoryResponse(
            "CHARGE",
            new BigDecimal("50000.00"),
            new BigDecimal("100000.00"),
            LocalDateTime.now().minusDays(1)),
        new BalanceHistoryResponse(
            "PAYMENT",
            new BigDecimal("-30000.00"),
            new BigDecimal("70000.00"),
            LocalDateTime.now().minusHours(5)))
        .stream().limit(limit).toList();

    return CommonResponse.success(history);
  }

  /**
   * 고유한 거래 ID 생성
   * 
   * 시스템 시간과 UUID를 조합하여 중복 방지를 보장한다.
   * 실제 운영에서는 분산 환경에서도 고유성이 보장되는 방식으로 구현한다.
   */
  private String generateTransactionId() {
    return "TXN_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }
}