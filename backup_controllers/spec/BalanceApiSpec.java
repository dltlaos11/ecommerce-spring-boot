package kr.hhplus.be.server.controller.spec;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.dto.balance.BalanceHistoryResponse;
import kr.hhplus.be.server.dto.balance.BalanceResponse;
import kr.hhplus.be.server.dto.balance.ChargeBalanceRequest;
import kr.hhplus.be.server.dto.balance.ChargeBalanceResponse;

/**
 * 잔액 관리 API 명세 인터페이스
 * 
 * 컨트롤러의 복잡성을 줄이고 API 문서화를 위한
 * Swagger 애노테이션을 분리하여 관리한다.
 * 
 * 구체적인 응답 예시는 구현체(BalanceController)에서 정의한다.
 */
@Tag(name = "사용자 잔액 관리", description = "사용자 잔액 충전, 조회 API")
public interface BalanceApiSpec {

  @Operation(summary = "잔액 조회", description = "사용자의 현재 잔액을 조회합니다.")
  CommonResponse<BalanceResponse> getBalance(
      @Parameter(description = "사용자 ID", example = "1") Long userId);

  @Operation(summary = "잔액 충전", description = "사용자의 잔액을 충전합니다.")
  CommonResponse<ChargeBalanceResponse> chargeBalance(
      @Parameter(description = "사용자 ID", example = "1") Long userId,
      @Valid ChargeBalanceRequest request);

  @Operation(summary = "잔액 변동 이력 조회", description = "사용자의 잔액 변동 이력을 조회합니다.")
  CommonResponse<List<BalanceHistoryResponse>> getBalanceHistory(
      @Parameter(description = "사용자 ID", example = "1") Long userId,
      @Parameter(description = "조회할 개수", example = "10") int limit);
}