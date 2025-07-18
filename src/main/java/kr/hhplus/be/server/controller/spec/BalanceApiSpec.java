package kr.hhplus.be.server.controller.spec;

import java.util.List;

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
import kr.hhplus.be.server.common.swagger.CommonApiResponses;
import kr.hhplus.be.server.dto.balance.BalanceHistoryResponse;
import kr.hhplus.be.server.dto.balance.BalanceResponse;
import kr.hhplus.be.server.dto.balance.ChargeBalanceRequest;
import kr.hhplus.be.server.dto.balance.ChargeBalanceResponse;

@Tag(name = "사용자 잔액 관리", description = "사용자 잔액 충전, 조회 API")
public interface BalanceApiSpec {

  @Operation(summary = "잔액 조회", description = "사용자의 현재 잔액을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "success": true,
            "data": {
              "userId": 1,
              "balance": 50000.00,
              "lastUpdated": "2025-07-17T10:30:00"
            },
            "timestamp": "2025-07-17T10:30:00"
          }
          """)))
  })
  @CommonApiResponses
  CommonResponse<BalanceResponse> getBalance(
      @Parameter(description = "사용자 ID", example = "1") Long userId);

  @Operation(summary = "잔액 충전", description = "사용자의 잔액을 충전합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "충전 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "success": true,
            "data": {
              "userId": 1,
              "previousBalance": 50000.00,
              "chargedAmount": 30000.00,
              "currentBalance": 80000.00,
              "transactionId": "tx_1721124600000"
            },
            "timestamp": "2025-07-17T10:30:00"
          }
          """))),
      @ApiResponse(responseCode = "409", description = "동시성 충돌", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "success": false,
            "error": {
              "code": "CONFLICT",
              "message": "동시 처리로 인한 충돌이 발생했습니다. 다시 시도해주세요."
            },
            "timestamp": "2025-07-17T10:30:00"
          }
          """)))
  })
  @CommonApiResponses
  CommonResponse<ChargeBalanceResponse> chargeBalance(
      @Parameter(description = "사용자 ID", example = "1") Long userId,
      @Valid ChargeBalanceRequest request);

  @Operation(summary = "잔액 변동 이력 조회", description = "사용자의 잔액 변동 이력을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "success": true,
            "data": [
              {
                "transactionType": "CHARGE",
                "amount": 50000.00,
                "balanceAfter": 100000.00,
                "createdAt": "2025-07-16T10:30:00"
              },
              {
                "transactionType": "PAYMENT",
                "amount": -30000.00,
                "balanceAfter": 70000.00,
                "createdAt": "2025-07-16T15:30:00"
              }
            ],
            "timestamp": "2025-07-17T10:30:00"
          }
          """)))
  })
  @CommonApiResponses
  CommonResponse<List<BalanceHistoryResponse>> getBalanceHistory(
      @Parameter(description = "사용자 ID", example = "1") Long userId,
      @Parameter(description = "조회할 개수", example = "10") int limit);
}