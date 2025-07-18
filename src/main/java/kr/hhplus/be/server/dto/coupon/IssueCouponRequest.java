
package kr.hhplus.be.server.dto.coupon;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;


// 쿠폰 발급 요청
@Schema(description = "쿠폰 발급 요청")
public class IssueCouponRequest {
    
    @NotNull(message = "사용자 ID는 필수입니다.")
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    
    // 기본 생성자
    public IssueCouponRequest() {}
    
    public IssueCouponRequest(Long userId) {
        this.userId = userId;
    }
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
}