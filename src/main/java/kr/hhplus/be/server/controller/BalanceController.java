package kr.hhplus.be.server.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    // Record 방식 생성자 사용 (순서 주의)
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

    // Record의 accessor 메서드 사용 (getAmount() 대신 amount())
    BigDecimal previousBalance = new BigDecimal("50000.00");
    BigDecimal currentBalance = previousBalance.add(request.amount());
    String transactionId = "tx_" + System.currentTimeMillis();

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
}