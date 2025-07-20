package kr.hhplus.be.server.common.swagger;

import java.lang.annotation.*;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import kr.hhplus.be.server.common.response.CommonResponse;

/**
 * 공통 API 응답 어노테이션
 * 
 * 모든 API에서 발생할 수 있는 기본적인 에러 응답(400, 404, 500)을 정의하여
 * 개별 컨트롤러에서 반복적인 선언을 방지한다.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class))),
    @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class))),
    @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class)))
})
public @interface CommonApiResponses {
}