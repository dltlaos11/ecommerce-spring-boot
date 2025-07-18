package kr.hhplus.be.server.common.swagger;

import java.lang.annotation.*;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import kr.hhplus.be.server.common.response.CommonResponse;

/**
 * 공통 API 응답 어노테이션
 * 모든 API에 공통으로 발생할 수 있는 응답 코드들을 정의
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
        {
          "success": false,
          "error": {
            "code": "VALIDATION_ERROR",
            "message": "입력 값이 올바르지 않습니다."
          },
          "timestamp": "2025-07-17T10:30:00"
        }
        """))),
    @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
        {
          "success": false,
          "error": {
            "code": "NOT_FOUND",
            "message": "요청한 리소스를 찾을 수 없습니다."
          },
          "timestamp": "2025-07-17T10:30:00"
        }
        """))),
    @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
        {
          "success": false,
          "error": {
            "code": "INTERNAL_ERROR",
            "message": "서버 내부 오류가 발생했습니다."
          },
          "timestamp": "2025-07-17T10:30:00"
        }
        """)))
})
public @interface CommonApiResponses {
}