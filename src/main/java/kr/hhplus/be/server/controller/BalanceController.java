package kr.hhplus.be.server.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.*;

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
  public CommonResponse<BalanceResponse> getBalance(@PathVariable Long userId) {
    BalanceResponse balance = new BalanceResponse(
        userId,
        new BigDecimal("50000.00"),
        LocalDateTime.now());
    return CommonResponse.success(balance);
  }

  @PostMapping("/{userId}/balance/charge")
  @Override
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